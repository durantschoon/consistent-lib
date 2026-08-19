;; clj-kondo hook for consistent-lib.impl/defwrapper.
;;
;; clj-kondo does not macroexpand, so without this hook every generated wrapper
;; is invisible to it: no var is registered, the parameter symbols look like
;; unresolved globals, and call sites get no arity checking at all. The hook
;; rewrites a `defwrapper` form into the `defn` it actually expands to, which
;; gives the generated wrappers exactly the static analysis hand-written defns
;; would have had — that is the point, not suppression.

(ns hooks.consistent-lib.impl
  (:require [clj-kondo.hooks-api :as api]))

(defn- variadic-params?
  "True if the parameter vector node declares a rest argument."
  [params-node]
  (boolean (some #(= '& %) (api/sexpr params-node))))

(defn- delegation-node
  "Builds the node for the wrapper body: the call that hands the arguments on
  to clojure.core, through `apply` when the wrapper is variadic."
  [core-sym-node params-node core-args-node]
  (let [target (api/token-node
                (symbol "clojure.core" (name (api/sexpr core-sym-node))))
        args (:children core-args-node)]
    (api/list-node
     (if (variadic-params? params-node)
       (list* (api/token-node 'clojure.core/apply) target args)
       (list* target args)))))

(defn- arity-node
  "Builds one `([params] body)` arity of the generated defn."
  [core-sym-node spec-node]
  (let [[params-node core-args-node] (:children spec-node)]
    (api/list-node
     [params-node (delegation-node core-sym-node params-node core-args-node)])))

(defn defwrapper
  "Rewrites (defwrapper name [core-sym] arity-spec+) into the equivalent defn."
  [{:keys [node]}]
  (let [[_ wrapper-name-node & more] (:children node)
        [core-sym-node spec-nodes] (if (api/vector-node? (first more))
                                     [wrapper-name-node more]
                                     [(first more) (rest more)])
        new-node (api/list-node
                  (list* (api/token-node 'clojure.core/defn)
                         wrapper-name-node
                         (map #(arity-node core-sym-node %) spec-nodes)))]
    {:node (with-meta new-node (meta node))}))
