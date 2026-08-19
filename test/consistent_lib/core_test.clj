(ns consistent-lib.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [consistent-lib.core :as c]))

(deftest partition-test
  (testing "basic partitioning"
    (is (= '((1 2) (3 4)) (c/partition [1 2 3 4] 2)))
    (is (= '((1 2 3) (4 5 6)) (c/partition [1 2 3 4 5 6] 3)))
    (is (= '() (c/partition [] 2))))
  (testing "partitioning with remainder"
    (is (= '((1 2) (3 4)) (c/partition [1 2 3 4 5] 2)))))

(deftest partition-all-test
  (testing "partition-all with complete groups"
    (is (= '((1 2) (3 4)) (c/partition-all [1 2 3 4] 2))))
  (testing "partition-all with incomplete final group"
    (is (= '((1 2) (3 4) (5)) (c/partition-all [1 2 3 4 5] 2)))))

(deftest partition-by-test
  (testing "partition by odd/even"
    (is (= '((1) (2) (3) (4)) (c/partition-by [1 2 3 4] odd?))))
  (testing "partition by identity"
    (is (= '((1 1) (2) (1) (2 2)) (c/partition-by [1 1 2 1 2 2] identity)))))

(deftest take-test
  (testing "taking elements"
    (is (= [1 2] (c/take [1 2 3 4] 2)))
    (is (= [] (c/take [1 2 3] 0)))
    (is (= [1 2 3] (c/take [1 2 3] 5)))))

(deftest drop-test
  (testing "dropping elements"
    (is (= [3 4] (c/drop [1 2 3 4] 2)))
    (is (= [1 2 3] (c/drop [1 2 3] 0)))
    (is (= [] (c/drop [1 2 3] 5)))))

(deftest take-nth-test
  (testing "taking every nth element"
    (is (= [1 3 5] (c/take-nth [1 2 3 4 5 6] 2)))
    (is (= [1] (c/take-nth [1] 2)))
    (is (= [1 4] (c/take-nth [1 2 3 4 5] 3)))))

(deftest take-while-test
  (testing "taking while predicate is true"
    (is (= [1 2] (c/take-while [1 2 3 4] #(< % 3))))
    (is (= [] (c/take-while [1 2 3] neg?)))
    (is (= [2 4 6] (c/take-while [2 4 6 7 8] even?)))))

(deftest drop-while-test
  (testing "dropping while predicate is true"
    (is (= [3 4] (c/drop-while [1 2 3 4] #(< % 3))))
    (is (= [1 2 3] (c/drop-while [1 2 3] neg?)))
    (is (= [7 8] (c/drop-while [2 4 6 7 8] even?)))))

(deftest split-at-test
  (testing "splitting at index"
    (is (= [[1 2] [3 4]] (c/split-at [1 2 3 4] 2)))
    (is (= [[] [1 2 3]] (c/split-at [1 2 3] 0)))
    (is (= [[1 2 3] []] (c/split-at [1 2 3] 5)))))
