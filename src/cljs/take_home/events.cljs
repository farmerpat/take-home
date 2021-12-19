(ns take-home.events
  (:require
    [cljs-time.core :as tc]
    [cljs-time.coerce :as tfc]
    [cljs-time.format :as tf]
    [re-frame.core :as rf]
    [ajax.core :as ajax]
    [reitit.frontend.easy :as rfe]
    [reitit.frontend.controllers :as rfc]
    [take-home.db :as db]))

(rf/reg-event-db
 :init-db
 (fn [_ _]
   (js/console.log "I R INIT DB!")
   db/default-db))

;;dispatchers

(rf/reg-event-db
  :common/navigate
  (fn [db [_ match]]
    (let [old-match (:common/route db)
          new-match (assoc match :controllers
                                 (rfc/apply-controllers (:controllers old-match) match))]
      (assoc db :common/route new-match))))

(rf/reg-fx
  :common/navigate-fx!
  (fn [[k & [params query]]]
    (rfe/push-state k params query)))

(rf/reg-event-fx
  :common/navigate!
  (fn [_ [_ url-key params query]]
    {:common/navigate-fx! [url-key params query]}))

(rf/reg-event-fx
 :repo-search-success
 (fn
   [{:keys [db]} [_ res]]
   {:dispatch [:get-latest-release (get res "owner") (get res "name")]}))

(rf/reg-event-fx
 :latest-release-got
 (fn [{:keys [db]} [_ res]]
   (let [release-notes (get res "release-notes")
         release-date (get res "release-date")
         name (get res "name")
         owner (get res "owner")]
     {:db (update-in db ["repos"] conj {:release-notes release-notes
                                        :release-date (tfc/from-string release-date)
                                        :owner owner
                                        :name name
                                        :seen false})
      ;:dispatch [:home/repo-list]
      })))

;; TODO:
;; one of these search events felt redundant at some point yesterday...
;; rm if applicable

(rf/reg-event-fx
 :get-latest-release
 (fn [{:keys [db]} [_ owner name]]
   {:http-xhrio {:method :get
                 :uri "/api/repo/release/latest"
                 :params {:name name :owner owner}
                 :request-format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords true})
                 :on-success [:latest-release-got]
                 :on-failure [:search-failure]}}))

(rf/reg-event-db
 :search-failure
 (fn [db [_ _]]
   (js/console.log "FAILURE!")
   db))

(rf/reg-event-fx
 :submit-search
 (fn
   [_ [_ repo-name]]
   {:http-xhrio {:method :get
                 :uri "/api/repo/search"
                 :params {:repo-name repo-name}
                 :request-format (ajax/json-request-format)
                 ;; :response-format (ajax/json-response-format {:keywords true})
                 :response-format (ajax/json-response-format {:keywords true})
                 :on-success [:repo-search-success]
                 :on-failure [:search-failure]}}))

(rf/reg-event-db
  :common/set-error
  (fn [db [_ error]]
    (assoc db :common/error error)))

(rf/reg-event-fx
 :page/init-home
 (fn [_ _]
   {}))

;;subscriptions

(rf/reg-sub
 ;;:home/repo-list
 :home/repo-list
 (fn [db _]
   (println "I R home/repo-list")
   (-> db :repos)))

(rf/reg-sub
  :common/route
  (fn [db _]
    (-> db :common/route)))

(rf/reg-sub
  :common/page-id
  :<- [:common/route]
  (fn [route _]
    (-> route :data :name)))

(rf/reg-sub
  :common/page
  :<- [:common/route]
  (fn [route _]
    (-> route :data :view)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))
