(ns take-home.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[take-home started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[take-home has shut down successfully]=-"))
   :middleware identity})
