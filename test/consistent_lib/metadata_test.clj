;; Metadata fidelity is guardrail 3: every public var carries a docstring and
;; accurate :arglists. These tests are deliberately independent of HOW the
;; wrappers are defined — they read the vars, not the source — so they keep
;; holding if the definition strategy ever changes again.

(ns consistent-lib.metadata-test
  (:require [clojure.test :refer [deftest is testing]]
            [consistent-lib.core]))

(def ^:private expected-public-count
  "The public surface of consistent-lib.core as of stage 02. Asserted so that
  adding or losing a wrapper is a deliberate, visible change."
  18)

(defn- public-vars
  "The public vars of consistent-lib.core, in a stable (name) order."
  []
  (->> (ns-publics 'consistent-lib.core)
       (sort-by key)
       (map val)))

(deftest public-surface-count-test
  (testing "consistent-lib.core exposes exactly the wrappers stage 02 ported"
    (is (= expected-public-count (count (public-vars))))))

(deftest every-public-var-has-a-docstring-test
  (doseq [wrapper-var (public-vars)]
    (testing (str wrapper-var)
      (let [docstring (:doc (meta wrapper-var))]
        (is (and (string? docstring) (seq docstring))
            (str wrapper-var " has no docstring"))))))

(deftest every-public-var-has-arglists-test
  (doseq [wrapper-var (public-vars)]
    (testing (str wrapper-var)
      (let [arglists (:arglists (meta wrapper-var))]
        (is (and (seq arglists) (every? vector? arglists))
            (str wrapper-var " has no :arglists"))))))

(deftest every-public-var-is-coll-first-test
  (testing "guardrail 2: the primary collection is argument 1 of every arity"
    (doseq [wrapper-var (public-vars)
            arglist (:arglists (meta wrapper-var))]
      (testing (str wrapper-var " " arglist)
        (is (contains? #{'coll '&} (first arglist))
            (str wrapper-var " does not take the collection first"))))))
