(ns take-home.git-get-test
  (:require
    [clojure.test :refer :all]
    [take-home.git-get :refer :all]))

(def real-repo {:name "vscode" :owner "microsoft"})
(def fake-repo {:name "a9HopeFULLYFakename" :owner "microsoft"})

(deftest test-git-get
  (testing "Existent repo exists"
    (= true (repo-exists? real-repo)))

  (testing "Non-existent repo does not exist"
    (= false (repo-exists? fake-repo))))
