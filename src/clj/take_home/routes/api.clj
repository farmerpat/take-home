(ns take-home.routes.api
  (:require
   [take-home.layout :as layout]
   [take-home.git-get :as gg]
   [clojure.java.io :as io]
   [take-home.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn repo-search [req]
  ;;TODO:
  ;;...Why not some nested map key presence predicate?
  ;;...Find and use or write.
  (if (not (and (contains? req :params)
                (contains? (:params req) :repo_name)))
    (response/ok
     {:body {:data nil
             :success false
             :message "Bad data received."}})
    (let [repo-name (:repo_name (:params req))]
      (if (gg/repo-exists? repo-name)
        (let [search-result (gg/search-repos-and-get-top-hit repo-name)]
          (println search-result)
          (response/ok {:body {:data search-result
                               :success true
                               :message "success"}}))))))

(defn repo-release [req]
  (if (not (and (contains? req :params)
                (contains? (:params req) :repo)))
    (response/ok
     {:body {:data nil
             :success false
             :message "Bad data received."}})
    (let [repo (:repo (:params req))]
      (if (not (and (contains? repo :name)
                    (contains? repo :owner)))
        (response/ok
         {:body {:data nil
                 :success false
                 :message "Repository missing owner or name."}})
        (let [release (gg/get-latest-release repo)]
          (response/ok
           {:body {:data release
                   :success true
                   :message "success"}}))))))

(defn api-routes []
  [ ""
   {:middleware [middleware/wrap-formats]}
   ["/api/repo/search" {:get repo-search}]
   ["/api/repo/release" {:get repo-release}]])
