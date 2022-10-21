/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.insa.beuvron.web.amour.guiFX.vues;

import fr.insa.beuvron.web.amour.bdd.GestionBdD;
import fr.insa.beuvron.web.amour.guiFX.JavaFXUtils;
import fr.insa.beuvron.web.amour.guiFX.VuePrincipale;
import fr.insa.beuvron.web.amour.model.Utilisateur;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 *
 * @author francois
 */
public class EnteteLogin extends HBox {

    private VuePrincipale main;

    private TextField tfNom;
    private PasswordField pfPass;

    public EnteteLogin(VuePrincipale main) {
        this.main = main;
        this.getChildren().add(new Label("nom :"));
        this.tfNom = new TextField();
        this.getChildren().add(this.tfNom);
        this.getChildren().add(new Label("pass :"));
        this.pfPass = new PasswordField();
        this.getChildren().add(this.pfPass);
        Button bLogin = new Button("Login");
        bLogin.setOnAction((t) -> {
            doLogin();
        });
        this.getChildren().add(bLogin);

    }

    public void doLogin() {
        String nom = this.tfNom.getText();
        String pass = this.pfPass.getText();
        try {
            Optional<Utilisateur> user
                    = GestionBdD.login(this.main.getBdd(), nom, pass);
            if (user.isEmpty()) {
                JavaFXUtils.showErrorInAlert("Erreur", "utilisateur invalide", "");
            } else {
                this.main.getSessionInfo().setCurUser(user);
                this.main.setEntete(new Label("connection OK"));
                this.main.setMainContent(new Label("vous êtes " + user));
            }

        } catch (SQLException ex) {
            JavaFXUtils.showErrorInAlert("Pb bdd", "Erreur",
                    ex.getMessage());
        }
    }

}
