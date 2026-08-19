;; Defining a Clojure library that redefines some common sequence operations 
;; with a consistent "main-problem-data" first argument order.

(ns consistent-lib.core
  (:refer-clojure :exclude [partition partition-all partition-by take drop
                            take-nth take-while drop-while split-at split-with
                            group-by interleave interpose concat some remove
                            subvec reduce]))

(defn partition
  "Partitions a collection into parts of size n. The main-problem-data (coll) comes first, followed by the size (n)."
  [coll n]
  (clojure.core/partition n coll))

(defn partition-all
  "Partitions a collection into parts of size n, allowing incomplete partitions at the end."
  [coll n]
  (clojure.core/partition-all n coll))

(defn partition-by
  "Partitions a collection by applying a function to the elements and splitting whenever the return value changes."
  [coll f]
  (clojure.core/partition-by f coll))

(defn take
  "Takes the first n elements of a collection."
  [coll n]
  (clojure.core/take n coll))

(defn drop
  "Drops the first n elements of a collection."
  [coll n]
  (clojure.core/drop n coll))

(defn take-nth
  "Takes every nth element from a collection."
  [coll n]
  (clojure.core/take-nth n coll))

(defn take-while
  "Takes elements from a collection while a predicate function returns true."
  [coll pred]
  (clojure.core/take-while pred coll))

(defn drop-while
  "Drops elements from a collection while a predicate function returns true."
  [coll pred]
  (clojure.core/drop-while pred coll))

(defn split-at
  "Splits a collection at a given index."
  [coll idx]
  (clojure.core/split-at idx coll))

(defn split-with
  "Splits a collection into two collections based on a predicate function."
  [coll pred]
  (clojure.core/split-with pred coll))

(defn group-by
  "Groups elements of a collection by the result of a function applied to each element."
  [coll f]
  (clojure.core/group-by f coll))

(defn interleave
  "Interleaves multiple collections together."
  [& colls]
  (apply clojure.core/interleave colls))

(defn interpose
  "Inserts an element between every item in a collection."
  [coll separator]
  (clojure.core/interpose separator coll))

(defn concat
  "Concatenates multiple collections."
  [& colls]
  (apply clojure.core/concat colls))

(defn some
  "Returns the first truthy value of applying the predicate to the collection."
  [coll pred]
  (clojure.core/some pred coll))

(defn remove
  "Removes elements of a collection for which a predicate returns true."
  [coll pred]
  (clojure.core/remove pred coll))

(defn subvec
  "Returns a subvector of the original vector between start and end indices."
  [coll start end]
  (clojure.core/subvec coll start end))

(defn reduce
  "Reduces a collection to a single value using a function. Optionally takes an initial value."
  ([coll f]
   (clojure.core/reduce f coll))
  ([coll f init]
   (clojure.core/reduce f init coll)))

;; Example usage
(comment
  (partition [1 2 3 4 5 6] 2)
  ;;=> ((1 2) (3 4) (5 6))

  (take [1 2 3 4 5] 3)
  ;;=> (1 2 3)

  (group-by [1 2 3 4 5 6] odd?))
  ;;=> {true [1 3 5], false [2 4 6]}
