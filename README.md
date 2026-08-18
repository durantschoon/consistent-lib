# consistent-lib

A Clojure library that provides consistent, collection-first versions of common sequence operations. While Clojure's core functions are powerful, they sometimes vary in their argument order. This library ensures a consistent interface where collection functions always take their primary operand (the collection) as their first argument.

## Motivation

In Clojure's core library, some functions take the collection as their first argument (like `map`), while others take it as their second argument (like `partition`). This inconsistency can make code less intuitive and harder to thread using `->` and `->>` macros. This library provides wrapped versions of these functions with a consistent argument order.

This library was inspired by the discussion in the [Clojure Google Group](https://groups.google.com/g/clojure/c/iyyNyWs53dc) about function argument ordering consistency.

## Installation

Add the following to your project.clj dependencies:

```clojure
[consistent-lib "0.1.0"]
```

## Usage

```clojure
(require '[consistent-lib.core :as c])

;; Instead of (partition 2 [1 2 3 4])
(c/partition [1 2 3 4] 2)
;; => ((1 2) (3 4))

;; Instead of (take-while odd? [1 3 4 5 7])
(c/take-while [1 3 4 5 7] odd?)
;; => (1 3)
```

### Thread-first Examples

The consistent argument order makes it natural to use the thread-first macro (`->`), leading to more readable data transformations:

```clojure
;; Without consistent-lib, mixing -> and ->> gets confusing:
(-> (range 10)
    (->> (partition 2))    ; have to switch to ->>
    (map inc)             ; back to ->
    (->> (partition-all 3))) ; switch to ->> again

;; With consistent-lib, everything flows naturally with ->:
(-> (range 10)
    (c/partition 2)
    (map inc)
    (c/partition-all 3))

;; More complex example showing natural data flow:
(-> [1 2 3 4 5 6 7 8 9 10]
    (c/partition-by odd?)     ; split into odd/even groups
    (c/take 3)               ; take first 3 groups
    (c/partition-all 2)      ; pair the groups
    flatten)
;; => (1 2 3 3 5)

;; Processing a sequence with multiple transformations:
(-> [0 1 2 3 4 5 6 7 8 9]
    (c/drop-while #(< % 3))  ; drop numbers less than 3
    (c/take 4)               ; take next 4 numbers
    (c/partition 2))         ; group in pairs
;; => ((3 4) (5 6))
```

## Available Functions

All functions take their collection argument first:

- `partition`: Partition a collection into n-sized chunks
- `partition-all`: Like partition but includes partial chunks
- `partition-by`: Partition collection by a function
- `take`: Take n items from a collection
- `drop`: Drop n items from a collection
- `take-nth`: Take every nth item
- `take-while`: Take items while predicate is true
- `drop-while`: Drop items while predicate is true
- `split-at`: Split collection at index

## Development

### Running Tests

The project uses `clojure.test`. To run the tests:

1. Using Leiningen:
   ```bash
   lein test
   ```

2. Using Clojure CLI:
   ```bash
   clj -X:test
   ```

3. From the REPL:
   ```clojure
   (require 'consistent-lib.core-test)
   (run-tests 'consistent-lib.core-test)
   ```

## License

Copyright 2024

Distributed under the Eclipse Public License version 1.0.
