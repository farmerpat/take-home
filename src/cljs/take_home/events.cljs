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
   db/default-db))

(rf/reg-event-fx
 :repo-search-success
 (fn
   [{:keys [db]} [_ res]]
   {:dispatch [:get-latest-release (get res "owner") (get res "name")]}))

;; (rf/reg-event-fx
;;  :latest-release-got
;;  (fn [{:keys [db]} [_ res]]
;;    (let [release-notes (get res "release-notes")
;;          release-date (get res "release-date")
;;          name (get res "name")
;;          owner (get res "owner")]
;;      {:db (update-in db [:repos] conj {:release-notes release-notes
;;                                         :release-date (tfc/from-string release-date)
;;                                         :owner owner
;;                                         :name name
;;                                         :seen false})})))

(rf/reg-event-db
 :latest-release-got
 (fn [db [_ res]]
   (let [release-notes (get res "release-notes")
         release-date (get res "release-date")
         name (get res "name")
         owner (get res "owner")]
     ;; (clj->js (println res))
     ;; (clj->js (println name))
     ;; (clj->js (println owner))
     ;; (clj->js (println release-date))
     (update-in db [:repos] conj {:release-notes release-notes
                                  :release-date (tfc/from-string release-date)
                                  :owner owner
                                  :name name
                                  :seen false}))))

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
                 :response-format (ajax/json-response-format {:keywords true})
                 :on-success [:repo-search-success]
                 :on-failure [:search-failure]}}))

(rf/reg-event-db
  :common/set-error
  (fn [db [_ error]]
    (assoc db :common/error error)))

(rf/reg-sub
 :repo-list
 (fn [db _]
   (:repos db)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))
