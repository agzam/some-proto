((nil . ((cider-preferred-build-tool . clojure-cli)
         (cider-clojure-cli-aliases . "dev:backend:frontend")
         (cider-default-cljs-repl . custom)
         (cider-offer-to-open-cljs-app-in-browser . nil)
         (eval . (progn
                   (make-variable-buffer-local 'cider-custom-cljs-repl-init-form)
                   (setq cider-custom-cljs-repl-init-form "(user/cljs-repl)")
                   (make-variable-buffer-local 'cider-jack-in-nrepl-middlewares)
                   (add-to-list 'cider-jack-in-nrepl-middlewares
                                "shadow.cljs.devtools.server.nrepl/middleware")))))
 (clojure-mode . ((eval . (progn
                            (define-clojure-indent
                             ;; latte.core for Cypress tests
                             (describe '(:defn))
                             (beforeEach '(:defn))
                             (it '(:defn))

                             ;; re-frame
                             (reg-event-db :defn)
                             (reg-event-fx :defn)
                             (reg-sub :defn)
                             (reg-fx :defn)
                             (reg-cofx :defn)
                             (re-frame.core/reg-event-db :defn)
                             (re-frame.core/reg-event-fx :defn)
                             (re-frame.core/reg-sub :defn)
                             (re-frame.core/reg-fx :defn)
                             (re-frame.core/reg-cofx :defn)))))))
