(ns example.validate2
  (:use easyconf.core))

(defn valid? [a] (if (> a 99) "must less than 100"))

;validator return a string msg, if not nil, than complie fail.
(defconf ^{:validator valid?} a 3)
