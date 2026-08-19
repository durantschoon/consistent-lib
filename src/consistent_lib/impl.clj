;; Wrapper generation for consistent-lib.
;;
;; The library's unifying rule is that a wrapper is a pure, transparent rename of
;; its clojure.core counterpart and that argument order is the ONLY permitted
;; difference (docs/stages/README.md, guardrail 1). `defwrapper` encodes exactly
;; that much freedom and no more: it can permute the arguments of a core fn, and
;; it can do nothing else. A wrapper therefore cannot quietly grow behavior of its
;; own, and its docstring and :arglists are derived rather than transcribed, so
;; they cannot drift away from the var being wrapped.

(ns consistent-lib.impl
  "Macro support for defining coll-first wrappers over clojure.core.

  Consumers of the library never need this namespace; it exists so that
  `consistent-lib.core` can declare wrappers as data (name + argument
  permutation) instead of hand-writing delegation, docstrings and :arglists.")

(defn- core-var
  "Resolves the clojure.core var named by `core-sym`.

  `core-sym` may be bare (`partition`) or qualified (`clojure.core/partition`);
  only its name is used, because a wrapper always wraps the core var of the same
  name. Throws at macroexpansion time if no such var exists, which turns a typo
  into a compile error rather than a silently doc-less wrapper."
  [core-sym]
  (let [qualified (symbol "clojure.core" (name core-sym))]
    (or (resolve qualified)
        (throw (ex-info (str "defwrapper: no such var " qualified)
                        {:core-sym core-sym})))))

(defn- coll-first-note
  "The one-line note appended to every derived docstring."
  [core-sym]
  (str "consistent-lib: coll-first wrapper for clojure.core/" (name core-sym)
       " — same behavior, collection first (see :arglists above)."))

(defn- derived-docstring
  "Builds a wrapper docstring from the wrapped core var's own docstring.

  The core text is reproduced verbatim so the wrapper never documents behavior
  the core fn does not have; the coll-first note is the only addition."
  [core-sym]
  (let [core-doc (:doc (meta (core-var core-sym)))]
    (if core-doc
      (str core-doc "\n\n  " (coll-first-note core-sym))
      (coll-first-note core-sym))))

(defn- rest-arg?
  "True if `sym` is the variadic marker in a parameter vector."
  [sym]
  (= '& sym))

(defn- variadic-params?
  "True if `params` declares a rest argument."
  [params]
  (boolean (some rest-arg? params)))

(defn- validate-permutation!
  "Asserts that `core-args` is a permutation of `params` and nothing else.

  This is guardrail 1 made mechanical: if a spec tried to drop an argument,
  invent one, or pass a computed value through, the sets would differ and the
  wrapper would fail to compile."
  [wrapper-name params core-args]
  (let [declared (set (remove rest-arg? params))
        passed (set core-args)]
    (when-not (= declared passed)
      (throw (ex-info (str "defwrapper " wrapper-name
                           ": delegation args must be a permutation of the "
                           "parameters (argument order is the only permitted "
                           "difference)")
                      {:wrapper wrapper-name
                       :params params
                       :core-args core-args})))))

(defn- delegation-form
  "Builds the body that hands `core-args` to the wrapped core fn.

  A variadic wrapper delegates through `apply`, since its rest parameter is
  already a seq of the remaining arguments."
  [core-sym params core-args]
  (let [target (symbol "clojure.core" (name core-sym))]
    (if (variadic-params? params)
      (list* `apply target core-args)
      (list* target core-args))))

(defmacro defwrapper
  "Defines a coll-first wrapper for a clojure.core fn.

  Each `arity-spec` is a pair of vectors `[params core-args]`: `params` is the
  wrapper's parameter vector (collection first), and `core-args` lists those same
  parameters in the order clojure.core expects them. `core-args` must be a
  permutation of `params` — that restriction is what keeps a wrapper a rename.

  `core-sym` defaults to `wrapper-name`, since a wrapper is named after the var
  it wraps; pass it explicitly only when the two differ.

  The generated var carries a docstring derived from the wrapped var's own
  docstring plus a coll-first note, and literal :arglists describing the
  wrapper's own argument order.

      (defwrapper partition
        [[coll n] [n coll]])

      (defwrapper reduce
        [[coll f]      [f coll]]
        [[coll f init] [f init coll]])"
  {:arglists '([wrapper-name arity-spec+]
               [wrapper-name core-sym arity-spec+])}
  [wrapper-name & more]
  (let [[core-sym arity-specs] (if (symbol? (first more))
                                 [(first more) (rest more)]
                                 [wrapper-name more])
        _ (doseq [[params core-args] arity-specs]
            (validate-permutation! wrapper-name params core-args))
        arglists (apply list (map (comp vec first) arity-specs))
        bodies (map (fn [[params core-args]]
                      (list (vec params)
                            (delegation-form core-sym params core-args)))
                    arity-specs)]
    `(defn ~(with-meta wrapper-name
              {:doc (derived-docstring core-sym)
               :arglists (list 'quote arglists)})
       ~@bodies)))
