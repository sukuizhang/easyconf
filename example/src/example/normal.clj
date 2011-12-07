(ns example.normal
  (:use easyconf.core))

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
