;; TODO

;; Copyright (C) 2019- Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

;; This program is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

;; This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.

;; Additional permission under GNU AGPL version 3 section 7.

;; If you modify this Program, or any covered work, by linking or combining it with any library (or a modified version of that library), containing parts covered by the terms of EPL v 1.0, the licensors of this Program grant you additional permission to convey the resulting work. Your modified version must prominently offer all users interacting with it remotely through a computer network (if your version supports such interaction) an opportunity to receive the Corresponding Source of your version by providing access to the Corresponding Source from a network server at no charge, through some standard or customary means of facilitating copying of software. Corresponding Source for a non-source form of such a combination shall include the source code for the parts of the libraries (dependencies) covered by the terms of EPL v 1.0 used as well as that of the covered work.

(ns valueflows.test.scenarios.track-and-trace
  (:require [midje.sweet :refer [against-background before after facts fact =>]]

            [valueflows
             [stores :refer [stores]]]
            [valueflows.db
             [mutations :as mut]
             [queries :as q]]

            [clj-storage.core :as storage :refer [Store]]
            [clj-time.core :as time]
            monger.joda-time
            [mount.core :as mount]
            [taoensso.timbre :as log]
            ))

(def actors #{:waste-management :retail-shop :textile-lab})
(def units #{:kilo :each :hour})

(against-background [(before :contents (mount/start-with-args {:port 8888}))
                     (after :contents (do
                                        (storage/empty-db-stores! stores)
                                        (mount/stop)))]

                ; WIP
                ; * if an economic event fulfills an intent, needs to be created a satisfaction to record that the intent is fulfilled, but not required for this demo
                ; * We'll not include commitments in this first scenario, we may want to track commitment later
                ; * If an intent involves an exchange , it will require a more complext intent structure, we'll need a proposal that involve 2 intents connected each other
                    (facts "Intents & simple process scenario"
                                        ; Current scenario will allow to:
                        ; - define and satisfy intents
                        ; - Create economic events that affect resources
                        ; - Create Process to group a bunch of economic events related each other
                    ; Some implementations of the scenario are:
                        ; - Show where resources are located on a map
                        ; - Track all the resource flow
                        ; - Broadcast available resources on the network
                        ; - Transfer resources
                           (fact "An unknown agent transfer some textile material to Waste Management"
                                 (mut/create-economic-event {
                                                             :receiver :waste-management
                                                             :has-point-in-time (time/now)
                                                             :action :transfer
                                                             :resource-quantity-has-numerical-value 200
                                                             :resource-quantity-has-unit :kilo
                                                             :to-resource-inventoried-as "Lot 173 textile material"
                                                             :resource-conforms-to :textile-material
                                                             :current-location "52.372807,4.8981023"
                                                             :resource-classified-as [:red :cotton]})
                                 (mut/create-economic-event {
                                                             :receiver :waste-management
                                                             :has-point-in-time (time/now)
                                                             :action :transfer
                                                             :resource-quantity-has-numerical-value 200
                                                             :resource-quantity-has-unit :kilo
                                                             :to-resource-inventoried-as "Lot 173 textile material"
                                                             :resource-conforms-to :textile-material
                                                             :current-location "52.372807,4.8981023"
                                                             :resource-classified-as [:red :cotton]})
                                 (mut/create-economic-event {
                                                             :provider :waste-management
                                                             :receiver :textile-lab
                                                             :has-point-in-time (time/now)
                                                             :action :transfer
                                                             :resource-inventoried-as "Lot 173 textile material"
                                                             :resource-quantity-has-numerical-value 100
                                                             :resource-quantity-has-unit :kilo
                                                             :to-resource-inventoried-as "Processed textile"
                                                             :resource-conforms-to :textile-material
                                                             :current-location "52.372807,4.8981023"
                                                             :resource-classified-as [:red :cotton]})
                                 (-> (q/query-resource {:name "Lot 173 textile material"})
                                     :resource-quantity-has-numerical-value
                                     ) => 300
                                 (-> (q/query-resource {:name "Lot 173 textile material"})
                                     :current-location) => "52.372807,4.8981023"
                                 (-> (q/query-economic-event {:receiver "waste-management"})
                                     first
                                     :to-resource-inventoried-as) =>  "Lot 173 textile material"
                                 )

(fact "Waste Management broadcasts an offer to transfer Lot 173 textile material"
                                 (mut/create-intent {
                                                     :provider :waste-management
                                                     :action :transfer
                                                     :available-quantity-has-numerical-value 200
                                                     :available-quantity-has-unit :kilo
                                                     :resource-conforms-to :textile-material
                                                     :resource-classified-as [:red :cotton]
                                                     :at-location "52.372807,4.8981023"
                                                     :description "Available to transfer a lot of red cotton textile material in good condition"})
                                 (-> (q/query-intent {:provider "waste-management"})
                                     first
                                     :resource-conforms-to
                                     ) => "textile-material")

(fact "Waste Management transfers part of Lot 173 to Textile Lab"
                                 (mut/create-economic-event {
                                                             :provider :waste-management
                                                             :receiver :textile-lab
                                                             :has-point-in-time (time/now)
                                                             :action :transfer
                                                             :resource-quantity-has-numerical-value 15
                                                             :satisfies 2
                                                             :resource-quantity-unit :kilo
                                                             :resource-inventoried-as "Lot 173 textile material"
                                                             :to-resource-inventoried-as "Raw red cotton"
                                                             :current-location "52.372807,4.8981023"
                                                             :resource-classified-as [:red :cotton]})
                                 (-> (q/query-economic-event {:receiver "textile-lab"})
                                     last
                                     :to-resource-inventoried-as) => "Raw red cotton"
                                 (-> (q/query-resource {:name "Raw red cotton"})
:resource-quantity-has-numerical-value
                                     ) => 15
                                 (-> (q/query-resource {:name "Lot 173 textile material"})
                                     :resource-quantity-has-numerical-value) => 285)
(fact "Textile Lab creates a new process to produce a new pair of jeans"
      (mut/create-process {
                           :name "Create a new pair of hyper jeans"
                           :note "Hyper jeans will be produced using the hyperballad design"
                           :before (time/now)})
      (-> (q/query-process)
          last
          :name
          ) => "Create a new pair of hyper jeans")


;; (fact "A bunch of work is done over the process: citing a design, consuming some materials and manufacturing the jeans, at the end the hyper jeans is produced and put in the inventory"
;; (mut/create-economic-event {
;;                                                           :provider :worker
;;                                                           :receiver :textile-lab
;;                                                           :has-point-in-time (time/now)
;;                                                           :action :cite
;;                                                           :input-of 4
;;                                                           :resource-quantity-has-numerical-value 1
;;                                                           :resource-quantity-unit :each
;;                                                           :resource-inventoried-as "hyperballad design"})
;;                               (mut/create-economic-event {
;;                                                           :provider :worker
;;                                                           :receiver :textile-lab
;;                                                           :has-point-in-time (time/now)
;;                                                           :action :consume
;;                                                           :input-of 4
;;                                                           :resource-quantity-has-numerical-value 10
;;                                                           :resource-quantity-has-unit :kilo
;;                                                           :resource-inventoried-as "Raw red cotton"})
;;
;;                               (mut/create-economic-event {
;;                                                           :provider :worker
;;                                                           :receiver :textile-lab
;;                                                           :has-point-in-time (time/now)
;;                                                           :action :work
;;                                                           :input-of 4
;;                                                           :effort-quantity-has-numerical-value 4
;;                                                           :effort-quantity-has-unit :hour
;;                                                           :resource-conforms-to "clothes manufacturing"
;;                                                           :note "Done all the work!"})
;;                               (mut/create-economic-event {
;;                                                           :provider :textile-lab
;;                                                           :receiver :textile-lab
;;                                                           :has-point-in-time (time/now)
;;                                                           :action :produce
;;                                                           :output-of 4
;;                                                           :current-location "TextileLab place"
;;                                                           :resource-classified-as [:red :jeans :cotton :slim-fit]
;;                                                           :resource-quantity-has-numerical-value 1
;;                                                           :resource-quantity-has-unit :each
;;                                                           :resource-inventoried-as "hyper jeans"})
;;                               ; Check if the process stored correctly all the input events
;;                               (-> (q/query-process {:id 4})
;;                                   :inputs
;;                                   :id) => [5, 6, 7]
;;                               ; Check if the process stored correctly all the output events
;;                               (-> (q/query-process {:id 4})
;;                                   :outputs
;;                                   :id) => [8]
;;                               ; Check if the hyper jeans resource is produced correctly
;;                               (-> (q/query-resource {:name "hyper jeans"})
;;                                   :current-location) => "TextileLab place"
;;                               )

))

