(ns example.validate1
  (:use easyconf.core))

;hold a validator meta, if check fail, complie fail ...
(defconf ^{:validator string? :validate-msg "not a string"} x "xx")
