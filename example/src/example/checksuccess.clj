(ns example.checksuccess
  (:use easyconf.core)
  (:require [easyconf.confs :as confs]))


(defconf a 1)
(defconf b [])
(defconf c)

(defconf m {:a 1})

(defn get-m [] m)
(defnconf f [x] x)

(defn sum [n]
  (reduce (fn [a b] ( + a (f b))) (range n)))

(defconf x)
(defconf y "")
(defconf z)

;because all config item has used, and every config var has config
;it can successful complie
(confs/check)


