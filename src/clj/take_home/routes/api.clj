(ns take-home.routes.api
  (:require
   [clj-time.core :as t]
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

;; Is there a better way to do this?
;; ASK!
(defn transform-value [transformer key m]
  (into {}
        (map
         (fn [[k v]]
           (if (= k key)
             [k
              (transformer v)]
             [k v]))
         m)))

(defn contains-all? [m ks]
  (every? (fn [k] (contains? m k)) ks))

(defn valid-request?
  ([req] (contains? req :params))
  ([req ks]
   (and (contains? req :params)
        (if (sequential? ks)
          (contains-all? (:params req) ks)
          (contains? (:params req) ks)))))

(defn repo-old-search-full [req]
  (if (not (valid-request? req :repo-name))
    (response/ok {})
    (let [repo-name (:repo-name (:params req))]
      (if (gg/repo-exists? repo-name)
        (let [search-result (gg/search-repos-and-get-top-hit repo-name)]
          (if (not (contains-all? search-result [:owner :name]))
            (response/ok {})
            (let [releases (gg/get-releases search-result)]
              (if (nil? releases)
                (response/ok {})
                (let [oldest_release (last releases)]
                  (response/ok
                   (merge (->> {:release-date (:published_at oldest_release)
                                :release-notes (:body oldest_release)}
                               (transform-value md-to-html-string :release-notes))
                          search-result)))))))))))

(defn repo-search-full [req]
  (if (not (valid-request? req :repo-name))
    (response/ok {})
    (let [repo-name (:repo-name (:params req))]
      (if (gg/repo-exists? repo-name)
        (let [search-result (gg/search-repos-and-get-top-hit repo-name)]
          (if (not (contains-all? search-result [:owner :name]))
            (response/ok {})
            (let [release (gg/get-latest-release search-result)]
              (if (nil? release)
                (response/ok
                 (merge {:release-notes "None Found"
                         :release-date (str (t/date-time 1970 1 1))}
                        search-result))
                (response/ok
                 (merge (transform-value md-to-html-string :release-notes release)
                        search-result))))))))))

(defn repo-search [req]
  (if (not (valid-request? req :repo-name))
    (response/ok
     {:body {:data nil
             :success false
             :message "Bad data received."}})
    (let [repo-name (:repo-name (:params req))]
      (if (gg/repo-exists? repo-name)
        (let [search-result (gg/search-repos-and-get-top-hit repo-name)]
          (response/ok search-result))))))

(defn repo-release-earliest [req]
  (if (not (and (valid-request? req :repo)
                (contains-all? (:repo (:params req)) [:name :owner])))

    (response/ok
     {:body {:data nil
             :success false
             :message "Bad data received."}})
    (let [releases (gg/get-releases (:repo (:params req)))]
      (if (nil? releases)
        (response/ok {:body {:data [] :success false :message "No releases found"}})
        (let [oldest_release (last releases)]
          (response/ok
           {:body {:data (->> {:release-date (:published_at oldest_release)
                               :release-notes (:body oldest_release)}
                              (transform-value md-to-html-string :release-notes)
                              keys-to-js-keys)
                   :success true
                   :message "success"}}))))))

(defn repo-release-latest [req]
  (if (not (valid-request? req [:owner :name]))
    (response/ok
     {:body {:data nil
             :success false
             :message "Bad data received."}})
    (let [params (:params req) ]
      (if (not (contains-all? params [:name :owner]))
        (response/ok
         {:body {:data nil
                 :success false
                 :message "Repository missing owner or name."}})
        (let [release (gg/get-latest-release params)]
          (response/ok
           (merge (transform-value md-to-html-string :release-notes release)
                  params)))))))

(defn get-update-repos [repos]
  (map (fn [repo]
         (merge {:name (:name repo), :owner (:owner repo)}
                (->> (gg/get-latest-release repo)
                     (transform-value md-to-html-string :release-notes))))
       repos))

(defn map-of-seqs->seq-of-maps [am]
  (map
   (fn [n]
     {:name (nth (:name am) n)
      :owner (nth (:owner am) n)
      :release-date (nth (:release-date am) n)
      :release-notes (nth (:release-notes am) n)
      :seen (nth (:seen am) n)
      :selected-for-detail-view
      (nth (:selected-for-detail-view am) n)})
   (range (count (:name am)))))

(defn repos-release [req]
  ;; If there is only one
  (if (string? (:name (:params req)))
    (response/ok
     {:releases (get-update-repos [(:params req)])})
    ;; for whatever reason, the data is getting here like this:
    ;; {:key1 [value_from_map0 value_from_map1] ...}
    ;; instead of like this:
    ;; [{:key1 value_from_map1}, {:key1 value_from_map2}]
    (let [repos (map-of-seqs->seq-of-maps (:params req))]
      (let [releases (get-update-repos repos)]
        (response/ok
         {:releases releases})))))

(defn api-routes []
  [ ""
   {:middleware [middleware/wrap-formats]}
   ["/api/repo/search" {:get repo-search}]
   ["/api/repo/searchfull" {:get repo-search-full}]
   ["/api/repo/searchfullold" {:get repo-old-search-full}]
   ["/api/repo/release/latest" {:get repo-release-latest}]
   ["/api/repo/release/earliest" {:get repo-release-earliest}]
   ["/api/repos/release" {:get repos-release}]])
