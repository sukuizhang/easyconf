(ns example.checkfail
  (:use easyconf.core)
  (:require [easyconf.confs :as confs]))

;defconf a is 1
(defconf a 1)
;you get a is 1000, becuase in a.properties,you define a=1000
(println (str "a is " a " "))

;defconf b is empty string
(defconf b "")

;now b is string \"[1 2]\"
(println (str "b is" b " type of b is:" (type b)))

(defconf b [])
;now b is a vector [1 2]
(println (str "b is" b " type of b is:" (type b)))

(defconf m {:a 1})

(defn get-m [] m)
(println (str "use it in function, m is:" (get-m)))

(defconf xyz 3)
(println "xyz do not config, so xyz is:" xyz)

(defnconf f [x] x)

(defn sum [n]
  (reduce (fn [a b] ( + a (f b))) (range n)))

(println (str "because f is replace of config, so now sum is:" (sum 10)))

;because check options has open in etc/ops/ops.properties
;because not provide xyz config item
;because config item x y z and c never used
;so it cause a complie error...
;normally, you can write it in your main.clj, after all unit has load in.
;then you can check if your config is correctly.
(confs/check)