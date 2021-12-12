(ns take-home.routes.api
  (:require
   [take-home.layout :as layout]
   [take-home.git-get :as gg]
   [take-home.middleware :as middleware]
   [clojure.java.io :as io]
   [markdown.core :refer [md-to-html-string]]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn drop-leading-colon [s]
  (if (= (first s) \:)
    (apply str (drop 1 s))
    s))

;; NOTE:
;; maybe better is this functionality as a middleware
(defn keys-to-js-keys [m]
  (into {}
        (map
         (fn [[k v]]
           [(keyword
             (clojure.string/replace
              (drop-leading-colon (str k)) "-" "_"))
            v])
         m)))

(defn transform-value [transformer key m]
  (into {}
        (map
         (fn [[k v]]
           (if (= k key)
             [k
              (transformer v)]
             [k v]))
         m)))

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
          (response/ok {:body {:data (keys-to-js-keys search-result)
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
           {:body {:data (->> release
                              (transform-value md-to-html-string :release-notes)
                              keys-to-js-keys)
                   :success true
                   :message "success"}}))))))

(defn api-routes []
  [ ""
   {:middleware [middleware/wrap-formats]}
   ["/api/repo/search" {:get repo-search}]
   ["/api/repo/release" {:get repo-release}]])
