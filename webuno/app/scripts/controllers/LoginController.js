'use strict';

angular.module('unoApp')
    /**
     * Contrôleur LoginController de la route /login
     * Gère la connexion utilisateur
     */
    .controller('LoginController', ['$rootScope', '$scope', '$state', '$translate', 'Auth', function ($rootScope, $scope, $state, $translate, Auth) {
        // Utilisation du service Auth qui retourne si l'utilisateur est connecté
        // Si un utilisateur est déjà authentifié
        // alors il est redirigé vers la page d'accueil de l'application
        if (Auth.isConnected()) {
            $state.go('app.home');
        }

        $scope.newUser = {};
        $scope.error = '';

        // Fonction qui permet de se connecter avec un pseudo et un mot de passe
        // Cette fonction est appelée à l'envoi du formulaire html
        $scope.goLogin = function () {
            // Utilisation du service Auth qui permet de connecter un utilisateur en renvoyant un token
            Auth.setUser($scope.newUser, function (data) {
                    data.name = data.playerName;
                    // Utilisation du service Auth pour setter les informations utilisateur dans la session
                    Auth.connectUser(data);
                    $state.go('app.home');
                }, function () {
              $scope.error = 'Erreur de connexion';
            });
        };

        // Fonction qui permet de se connecter en tant qu'invité avec un mot de passe généré aléatoirement
        $scope.goLoginGuess = function () {
            var name = 'Anonymous' + Math.floor((Math.random() * (1000 - 1) + 1));
            // Utilisation du service Auth qui permet de connecter un utilisateur en tant qu'invité en renvoyant un token
            Auth.setUserGuess(name, function (data) {
                    data.name = name;
                    // Utilisation du service Auth pour setter les informations utilisateur dans la session
                    Auth.connectUser(data);
                    $state.go('app.home');
                });
        };
    }]);
