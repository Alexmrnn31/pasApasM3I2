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
package fr.insa.beuvron.cours.m3.pasApasM3I2;

import fr.insa.beuvron.utils.ConsoleFdB;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author francois
 */
public class BDD {

    public static Connection connectGeneralPostGres(String host,
            int port, String database,
            String user, String pass)
            throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        Connection con = DriverManager.getConnection(
                "jdbc:postgresql://" + host + ":" + port
                + "/" + database,
                user, pass);
        con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        return con;
    }

    public static void creeSchema(Connection con)
            throws SQLException {
        // je veux que le schema soit entierement créé ou pas du tout
        // je vais donc gérer explicitement une transaction
        con.setAutoCommit(false);
        try ( Statement st = con.createStatement()) {
            // creation des tables
            st.executeUpdate(
                    """
                    create table utilisateur (
                        id integer not null primary key
                        generated always as identity,
                        nom varchar(30) not null,
                        pass varchar(30) not null
                    )
                    """);
            st.executeUpdate(
                    """
                    create table aime (
                        u1 integer not null,
                        u2 integer not null
                    )
                    """);
            // je defini les liens entre les clés externes et les clés primaires
            // correspondantes
            st.executeUpdate(
                    """
                    alter table aime
                        add constraint fk_aime_u1
                        foreign key (u1) references utilisateur(id)
                    """);
            st.executeUpdate(
                    """
                    alter table aime
                        add constraint fk_aime_u2
                        foreign key (u2) references utilisateur(id)
                    """);
            // si j'arrive jusqu'ici, c'est que tout s'est bien passé
            // je confirme (commit) la transaction
            con.commit();
            // je retourne dans le mode par défaut de gestion des transaction :
            // chaque ordre au SGBD sera considéré comme une transaction indépendante
            con.setAutoCommit(true);
        } catch (SQLException ex) {
            // quelque chose s'est mal passé
            // j'annule la transaction
            con.rollback();
            // puis je renvoie l'exeption pour qu'elle puisse éventuellement
            // être gérée (message à l'utilisateur...)
            throw ex;
        }
    }

    // vous serez bien contents, en phase de développement de pouvoir
    // "repartir de zero" : il est parfois plus facile de tout supprimer
    // et de tout recréer que d'essayer de modifier le schema et les données
    public static void deleteSchema(Connection con) throws SQLException {
        try ( Statement st = con.createStatement()) {
            // pour être sûr de pouvoir supprimer, il faut d'abord supprimer les liens
            // puis les tables
            // suppression des liens
            try {
                st.executeUpdate(
                        """
                    alter table aime
                        drop constraint fk_aime_u1
                             """);
            } catch (SQLException ex) {
                // nothing to do : maybe the constraint was not created
            }
            try {
                st.executeUpdate(
                        """
                    alter table aime
                        drop constraint fk_aime_u2
                    """);
            } catch (SQLException ex) {
                // nothing to do : maybe the constraint was not created
            }
            // je peux maintenant supprimer les tables
            try {
                st.executeUpdate(
                        """
                    drop table aime
                    """);
                // je defini les liens entre les clés externes et les clés primaires
                // correspondantes
            } catch (SQLException ex) {
                // nothing to do : maybe the table was not created
            }
            try {
                st.executeUpdate(
                        """
                    drop table utilisateur
                    """);
            } catch (SQLException ex) {
                // nothing to do : maybe the table was not created
            }
        }
    }

    public static void afficheTousLesUtilisateur(Connection con)
            throws SQLException {
        try ( Statement st = con.createStatement()) {
            ResultSet res = st.executeQuery("select * from utilisateur");
            while (res.next()) {
                String lenom = res.getString("nom");
                String pass = res.getString(2);
                System.out.println("utilisateur " + lenom + " ("
                        + pass + ")");
            }
        }
    }

    public static void afficheUtilisateurParNom(Connection con,
            String nom)
            throws SQLException {
        try ( PreparedStatement st = con.prepareStatement(
                "select * from utilisateur where nom = ?")) {
            st.setString(1, nom);
            ResultSet res = st.executeQuery();
            while (res.next()) {
                String lenom = res.getString("nom");
                String pass = res.getString(2);
                System.out.println("utilisateur " + lenom + " ("
                        + pass + ")");
            }
        }
    }

    // lors de la création d'un utilisateur, l'identificateur est automatiquement
    // créé par le SGBD.
    // on va souvent avoir besoin de cet identificateur dans le programme,
    // par exemple pour gérer des liens "aime" entre utilisateur
    // vous trouverez ci-dessous la façon de récupérer les identificateurs
    // créés : ils se présentent comme un ResultSet particulier.
    public static int createUtilisateur(Connection con, String nom,
            String pass)
            throws SQLException {
        // lors de la creation du PreparedStatement, il faut que je précise
        // que je veux qu'il conserve les clés générées
        con.setAutoCommit(false);
        try ( PreparedStatement pst2 = con.prepareStatement(
                "select id from utilisateur where nom = ?")) {
            pst2.setString(1, nom);
            ResultSet existe = pst2.executeQuery();
            if (!existe.next()) {
                try ( PreparedStatement pst = con.prepareStatement(
                        """
                insert into utilisateur (nom,pass) values (?,?)
                """, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    pst.setString(1, nom);
                    pst.setString(2, pass);
                    pst.executeUpdate();

                    // je peux alors récupérer les clés créées comme un result set :
                    try ( ResultSet rid = pst.getGeneratedKeys()) {
                        // et comme ici je suis sur qu'il y a une et une seule clé, je
                        // fait un simple next 
                        rid.next();
                        // puis je récupère la valeur de la clé créé qui est dans la
                        // première colonne du ResultSet
                        int id = rid.getInt(1);
                        return id;
                    }
                }
            } else {
                return -1;
            }
        }
    }

    public static void demandeNouvelUtilisateur(Connection con)
            throws SQLException {
        String nom = ConsoleFdB.entreeString("nom ?");
        String pass = ConsoleFdB.entreeString("pass ?");
        int id = createUtilisateur(con, nom, pass);
        if (id == -1) {
            System.out.println("utilisateur existe déjà");
        } else {
            System.out.println("utilisateur N° " + id + "créé");
        }
    }

    public static void recreeTout(Connection con) throws SQLException {
        deleteSchema(con);
        creeSchema(con);
    }

    public static void menu(Connection con) {
        int rep = -1;
        while (rep != 0) {
            System.out.println("Menu BdD Aime");
            System.out.println("=============");
            System.out.println("1) créer/recréer la BdD initiale");
            System.out.println("2) liste des utilisateurs");
            System.out.println("3) liste par nom");
//            System.out.println("3) liste des liens 'Aime'");
            System.out.println("4) ajouter un utilisateur");
//            System.out.println("5) ajouter un lien 'Aime'");
//            System.out.println("6) ajouter n utilisateurs aléatoires");
            System.out.println("0) quitter");
            rep = ConsoleFdB.entreeEntier("Votre choix : ");
            try {
                if (rep == 1) {
                    recreeTout(con);
                } else if (rep == 2) {
                    afficheTousLesUtilisateur(con);
                } else if (rep == 3) {
                    String lenom = ConsoleFdB.entreeString("donnez un nom");
                    afficheUtilisateurParNom(con, lenom);
                } else if (rep == 4) {
                    demandeNouvelUtilisateur(con);
//                } else if (rep == 5) {
//                    demandeNouvelAime(con);
//                } else if (rep == 6) {
//                    System.out.println("création d'utilisateurs 'aléatoires'");
//                    int combien = ConsoleFdB.entreeEntier("combien d'utilisateur : ");
//                    for (int i = 0; i < combien; i++) {
//                        boolean exist = true;
//                        while (exist) {
//                            String nom = "U" + ((int) (Math.random() * 10000));
//                            if (!nomUtilisateurExiste(con, nom)) {
//                                exist = false;
//                                createUtilisateur(con, nom, "P" + ((int) (Math.random() * 10000)));
//                            }
//                        }
//
//                    }
                }
            } catch (SQLException ex) {
                throw new Error(ex);
            }
        }
    }

    public static Connection defautConnect()
            throws ClassNotFoundException, SQLException {
        return connectGeneralPostGres("localhost", 5439, "postgres",
                "postgres", "pass");
    }

    public static void main(String[] args) {
        try ( Connection con = defautConnect()) {
            System.out.println("connecté !!!");
            menu(con);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

}
