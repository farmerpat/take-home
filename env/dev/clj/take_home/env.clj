(ns take-home.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [take-home.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[take-home started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[take-home has shut down successfully]=-"))
   :middleware wrap-dev})
