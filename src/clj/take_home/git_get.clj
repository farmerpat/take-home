(ns take-home.git-get
  (:require
   [clj-http.client :as http]
   [clojure.string :as string]
   [ring.util.codec :as ruc]))

(def BASE-URI "https://api.github.com")

(defn well-formed-repo? [repo]
  (and (contains? repo :owner)
       (contains? repo :name)))

;; TODO: proper logging/handling e.g. returning error info
(defn get-it
  ([] (get-it [] ""))
  ([path-parts] (get-it path-parts ""))
  ([path-parts query-string]
   (let [uri (str (string/join "/" (cons BASE-URI path-parts)) query-string)]
     (try
       (let [res (http/get uri {:accept :json :as :json})]
         (cond 
           (not (= 200 (:status res))) nil
           :else res))
       (catch Exception e nil)))))

(defn build-search-query-string [search-terms]
  (str "?q=" (ruc/url-encode (string/join " " search-terms))))

(defn repo-exists? [repo-name]
  (let [res (get-it ["search" "repositories" ] (build-search-query-string [repo-name]))]
    (cond (nil? res) false
          (not (contains? res :body)) false
          :else
          (let [results (:body res)]
            (if (= 0 (count (:items results)))
              false
              true)))))

;; TODO: more robust
(defn pluck-repo-from-url [url]
  (let [owner-name (take 2 (drop 4 (string/split url #"/")))]
    {:owner (first owner-name)
     :name (second owner-name)}))

(defn search-repos-and-get-top-hit [repo-name]
  (let [res (get-it ["search" "repositories" ] (build-search-query-string [repo-name]))]
    (cond (nil? res) nil
          (not (contains? res :body)) nil
          :else
          (let [results (:body res)]
            (if (= 0 (count (:items results)))
              nil
              (if (not (contains? results :items))
                nil
                (let [top-hit (first (:items results))]
                  (if (not (contains? top-hit :releases_url))
                    nil
                    (pluck-repo-from-url (:releases_url top-hit))))))))))

;; e.g.
;; {:owner "microsoft" :name "vscode"}
(defn get-latest-release
  "Given: a repo map with :name and :owner
   Upon success, returns a map with :release-notes and :release-date
   Upon failure, returns nil"
  [repo]
  (if (not (well-formed-repo? repo))
    nil
    (let [res (get-it ["repos" (:owner repo) (:name repo) "releases" "latest"])]
      (if (or (not (contains? res :body))
              (not (contains? (:body res) :body)))
        nil
        (let [release-notes (:body (:body res))
              release-date (:published_at (:body res))]
          {:release-notes release-notes
           :release-date release-date})))))

(defn get-releases [repo]
  (if (not (well-formed-repo? repo))
    nil
    (let [res (get-it ["repos" (:owner repo) (:name repo) "releases"])]
      (if (not (contains? res :body))
        nil
        (:body res)))))
