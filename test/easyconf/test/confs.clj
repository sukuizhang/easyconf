(ns easyconf.test.confs
  (:use [easyconf.confs])
  (:use [midje.sweet]))


(fact "test validate err"
  (validate-err ..var.. ..conf.. nil) => nil
  (validate-err ..var.. ..conf.. "validate fail!") => (throws Exception))

(fact "test set var" 
  @(set-var (load-string "(def x 1)") {:value 3}) => 3
  @(set-var (load-string "(def x 1)") {:value "5"}) => 5
  @(set-var (load-string "(def x 1)") {:value [1 2]})  => [1 2]
  (set-var (load-string "(def ^{:validator number?} x 1)") {:value [1 2]}) => (throws Exception)
  (set-var (load-string "(def ^{:validator string?} x \"1\")") {:value 1}) => (throws Exception))
