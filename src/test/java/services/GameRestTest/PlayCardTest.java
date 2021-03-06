package services.GameRestTest;

import fr.unice.idse.model.Game;
import fr.unice.idse.model.Model;
import fr.unice.idse.model.card.Card;
import fr.unice.idse.model.card.Color;
import fr.unice.idse.model.card.Value;
import fr.unice.idse.model.player.Player;
import fr.unice.idse.services.GameRest;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlayCardTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(GameRest.class);
    }

    public Model model;

    @Before
    public void init() {
        model = Model.getInstance();
        model.setGames(new ArrayList<Game>());
        model.setPlayers(new ArrayList<Player>());
        model.createPlayer("toto", "token");
        model.addGame(model.getPlayerFromList("token"), "tata", 4);
        for (int i = 0; i < 3; i++) {
            assertTrue(model.createPlayer("azert" + i, "token" + i));
            assertTrue(model.addPlayerToGame("tata", model.getPlayerFromList("token" + i)));
        }
    }

    @Test
    public void retourne405SiLaPartieNExistePas() throws JSONException {
        Response response = target("/game/test/john").request().header("token", "token").put(Entity.entity("{}", MediaType.APPLICATION_JSON));
        assertEquals(405, response.getStatus());
        // Parse la reponse en JSON
        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertEquals("The game does not exist", json.getString("error"));
    }

    @Test
    public void retourne405SiLaPartieNEstPasCommencee() throws JSONException {
        Response response = target("/game/tata/john").request().header("token", "token").put(Entity.entity("{}", MediaType.APPLICATION_JSON));
        assertEquals(405, response.getStatus());
        // Parse la reponse en JSON
        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertEquals("The game does hasn't begun", json.getString("error"));
    }

    @Test
    public void retourne405SiLeJoeurNExistePas() throws JSONException {
        assertTrue(model.findGameByName("tata").start());
        Response response = target("/game/tata/john").request().header("token", "token").put(Entity.entity("{}", MediaType.APPLICATION_JSON));
        assertEquals(405, response.getStatus());
        // Parse la reponse en JSON
        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertEquals("The player does not exist", json.getString("error"));
    }

    @Test
    public void retourne405SiLeJoeurNExistePasDansCettePartie() throws JSONException {
        // Init the game
        assertTrue(model.createPlayer("john", "tokenultime"));
        assertTrue(model.addGame(model.getPlayerFromList("tokenultime"), "test", 4));
        for (int i = 3; i < 6; i++) {
            assertTrue(model.createPlayer("azert" + i, "token" + i));
            assertTrue(model.addPlayerToGame("test", model.getPlayerFromList("token" + i)));
        }
        assertTrue(model.findGameByName("tata").start());

        Response response = target("/game/tata/azert4").request().header("token", "token4").put(Entity.entity("{}", MediaType.APPLICATION_JSON));

        assertEquals(405, response.getStatus());
        // Parse la reponse en JSON
        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertEquals("The player does not exist", json.getString("error"));
    }

    @Test
    public void retourne405SiLeJSONEnvoyerEstInvalide() throws JSONException {
        assertTrue(model.findGameByName("tata").start());
        Response response = target("/game/tata/toto").request().header("token", "token").put(Entity.entity("{}", MediaType.APPLICATION_JSON));
        assertEquals(405, response.getStatus());

        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertEquals("The json object does not follow the rules", json.getString("error"));
    }

    @Test
    public void retourne405SiLeJSONEnvoyerEstInvalideDusALaValueManquante() throws JSONException {
        assertTrue(model.findGameByName("tata").start());
        Response response = target("/game/tata/toto").request().header("token", "token").put(Entity.entity("{\"color\":\"Blue\"}", MediaType.APPLICATION_JSON));
        assertEquals(405, response.getStatus());

        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertEquals("The json object does not follow the rules", json.getString("error"));
        assertEquals(false, json.has("value"));
    }

    @Test
    public void retourne405SiLeJSONEnvoyerEstInvalideDusALaColorManquante() throws JSONException {
        assertTrue(model.findGameByName("tata").start());
        Response response = target("/game/tata/toto").request().header("token", "token").put(Entity.entity("{\"value\":\"2\"}", MediaType.APPLICATION_JSON));
        assertEquals(405, response.getStatus());

        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertEquals("The json object does not follow the rules", json.getString("error"));
        assertEquals(false, json.has("color"));
    }


    @Test
    public void retourne405SiLeJoeurNePeutPasJouer() throws JSONException {
        assertTrue(model.findGameByName("tata").start());
        Response response = target("/game/tata/azert2").request().header("token", "token2").put(Entity.entity("{\"value\":6, \"color\":\"Blue\"}", MediaType.APPLICATION_JSON));
        assertEquals(405, response.getStatus());
        // Parse la reponse en JSON
        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertEquals("The player can't play", json.getString("error"));
    }


    @Test
    public void retourne405SiLeJoueurNePossedePasLaCarte() throws JSONException {
        assertTrue(model.findGameByName("tata").start());
        model.findGameByName("tata").getActualPlayer().setCards(new ArrayList<Card>());
        model.findGameByName("tata").getActualPlayer().getCards().add(new Card(Value.Five, Color.Blue));
        model.findGameByName("tata").getActualPlayer().getCards().add(new Card(Value.Three, Color.Blue));
        model.findGameByName("tata").getStack().changeColor(Color.Red);
        model.findGameByName("tata").getStack().addCard(new Card(Value.Five, Color.Red));

        Response response = target("/game/tata/toto").request().header("token", "token").put(Entity.entity("{\"value\":Reverse, \"color\":\"Black\"}", MediaType.APPLICATION_JSON));
        assertEquals(405, response.getStatus());
        // Parse la reponse en JSON
        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertEquals("The player does not possese this card", json.getString("error"));
    }

    @Test
    public void retourne405SiLaCarteNEstPasJouable() throws JSONException {
        assertTrue(model.findGameByName("tata").start());
        // cartes du joueur actuel
        ArrayList<Card> cards = new ArrayList<>();
        cards.add(new Card(Value.Two, Color.Blue));
        cards.add(new Card(Value.Eight, Color.Blue));
        model.findGameByName("tata").getActualPlayer().setCards(cards);

        // Change la couleur du stack
        model.findGameByName("tata").getStack().changeColor(Color.Red);
        model.findGameByName("tata").setActualColor(Color.Red);

        // Cartes du stack
        ArrayList<Card> stack = new ArrayList<>();
        stack.add(new Card(Value.Eight, Color.Red));
        model.findGameByName("tata").getStack().setStack(stack);

        Response response = target("/game/tata/toto").request().header("token", "token").put(Entity.entity("{\"value\":Two, \"color\":\"Blue\"}", MediaType.APPLICATION_JSON));


        assertEquals(405, response.getStatus());
        // Parse la reponse en JSON
        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertEquals("The card can't be played", json.getString("error"));
    }

    @Test
    public void retourne200SiTouteLesConditionSontValider() throws JSONException {
        assertTrue(model.findGameByName("tata").start());
        model.findGameByName("tata").getActualPlayer().getCards().add(new Card(Value.Five, Color.Blue));
        model.findGameByName("tata").getStack().addCard(new Card(Value.Five, Color.Red));
        model.findGameByName("tata").setActualColor(Color.Blue);
        Response response = target("/game/tata/toto").request().header("token", "token").put(Entity.entity("{\"value\":Five, \"color\":\"Blue\"}", MediaType.APPLICATION_JSON));

        assertEquals(200, response.getStatus());
        // Parse la reponse en JSON
        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertTrue(json.getBoolean("success"));
    }

    @Test
    public void changementDeCouleurAvecUneMauvaiseCouleur() throws JSONException {
        assertTrue(model.findGameByName("tata").start());
        model.findGameByName("tata").getActualPlayer().getCards().add(new Card(Value.Wild, Color.Black));
        model.findGameByName("tata").getStack().addCard(new Card(Value.Five, Color.Red));
        model.findGameByName("tata").setActualColor(Color.Red);

        // Envoie des données
        JSONObject jsonEntity = new JSONObject();
        jsonEntity.put("value", "Wild");
        jsonEntity.put("color", "Black");
        jsonEntity.put("setcolor", "Black");
        Response response = target("/game/tata/toto").request().header("token", "token").put(Entity.entity(jsonEntity.toString(), MediaType.APPLICATION_JSON));

        assertEquals(405, response.getStatus());
        // Parse la reponse en JSON
        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertEquals("Setcolor not accepted", json.getString("error"));
    }

    @Test
    public void changementDeCouleurAvecUneBonneCouleur() throws JSONException {
        assertTrue(model.findGameByName("tata").start());
        model.findGameByName("tata").getActualPlayer().getCards().add(new Card(Value.Wild, Color.Black));
        model.findGameByName("tata").getStack().addCard(new Card(Value.Five, Color.Red));
        model.findGameByName("tata").setActualColor(Color.Red);

        // Envoie des données
        JSONObject jsonEntity = new JSONObject();
        jsonEntity.put("value", "Wild");
        jsonEntity.put("color", "Black");
        jsonEntity.put("setcolor", "Yellow");
        Response response = target("/game/tata/toto").request().header("token", "token").put(Entity.entity(jsonEntity.toString(), MediaType.APPLICATION_JSON));

        assertEquals(200, response.getStatus());
        // Parse la reponse en JSON
        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertTrue(json.getBoolean("success"));
        assertEquals("Yellow", model.findGameByName("tata").getActualColor().toString());
        assertEquals("azert0", model.findGameByName("tata").getActualPlayer().getName());
    }

    @Test
    public void changementDeCouleurAvecUnPlusQuatre() throws JSONException{
        assertTrue(model.findGameByName("tata").start());
        model.findGameByName("tata").getPlayers().get(0).getCards().add(new Card(Value.DrawFour, Color.Black));
        model.findGameByName("tata").getPlayers().get(1).getCards().add(new Card(Value.Five, Color.Red));

        model.findGameByName("tata").getStack().addCard(new Card(Value.Five, Color.Red));
        model.findGameByName("tata").setActualColor(Color.Red);
        assertEquals(8, model.findGameByName("tata").getPlayers().get(1).getCards().size());

        JSONObject jsonEntity = new JSONObject();
        jsonEntity.put("value", "DrawFour");
        jsonEntity.put("color", "Black");
        jsonEntity.put("setcolor", "Red");
        Response response = target("/game/tata/toto").request().header("token", "token").put(Entity.entity(jsonEntity.toString(), MediaType.APPLICATION_JSON));

        assertEquals(200, response.getStatus());
        // Parse la reponse en JSON
        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertTrue(json.getBoolean("success"));
        assertEquals("Red", model.findGameByName("tata").getActualColor().toString());
        assertEquals("azert0", model.findGameByName("tata").getActualPlayer().getName());

        jsonEntity.remove("value");
        jsonEntity.remove("color");
        jsonEntity.remove("setcolor");

        jsonEntity.put("value", "Five");
        jsonEntity.put("color", "Red");


        response = target("/game/tata/azert0").request().header("token", "token0").put(Entity.entity(jsonEntity.toString(), MediaType.APPLICATION_JSON));

        assertEquals(405, response.getStatus());
        json = new JSONObject(response.readEntity(String.class));
        assertEquals("The card can't be played", json.getString("error"));

        response = target("/game/tata/azert0").request().header("token", "token0").post(Entity.entity(jsonEntity.toString(), MediaType.APPLICATION_JSON));
        JSONObject jsonObject = new JSONObject(response.readEntity(String.class));

        assertTrue(jsonObject.getBoolean("return"));
        assertEquals(12, model.findGameByName("tata").getPlayers().get(1).getCards().size());
        assertEquals("azert1", model.findGameByName("tata").getActualPlayer().getName());
        assertEquals(1, model.findGameByName("tata").getCptDrawCard());
    }

    @Test
    public void jouerUnPlus2EtJoueurSuivantPioche() throws JSONException{
        assertTrue(model.findGameByName("tata").start());

        // Ajout des cartes aux joueurs
        model.findGameByName("tata").getPlayers().get(0).getCards().add(new Card(Value.DrawTwo, Color.Red));
        model.findGameByName("tata").getPlayers().get(1).getCards().add(new Card(Value.Five, Color.Red));

        // Ajout d'un couleur rouge au stack
        model.findGameByName("tata").getStack().addCard(new Card(Value.Five, Color.Red));
        model.findGameByName("tata").setActualColor(Color.Red);
        assertEquals(8, model.findGameByName("tata").getPlayers().get(1).getCards().size());


        JSONObject jsonEntity = new JSONObject();
        jsonEntity.put("value", "DrawTwo");
        jsonEntity.put("color", "Red");
        Response response = target("/game/tata/toto").request().header("token", "token").put(Entity.entity(jsonEntity.toString(), MediaType.APPLICATION_JSON));

        assertEquals(200, response.getStatus());
        // Parse la reponse en JSON
        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertTrue(json.getBoolean("success"));
        assertEquals("Red", model.findGameByName("tata").getActualColor().toString());
        assertEquals("azert0", model.findGameByName("tata").getActualPlayer().getName());

        jsonEntity.put("value", "Five");
        jsonEntity.put("color", "Red");

        response = target("/game/tata/azert0").request().header("token", "token0").put(Entity.entity(jsonEntity.toString(), MediaType.APPLICATION_JSON));

        assertEquals(405, response.getStatus());
        json = new JSONObject(response.readEntity(String.class));
        assertEquals("The card can't be played", json.getString("error"));

        response = target("/game/tata/azert0").request().header("token", "token0").post(Entity.entity(jsonEntity.toString(), MediaType.APPLICATION_JSON));
        JSONObject jsonObject = new JSONObject(response.readEntity(String.class));

        assertTrue(jsonObject.getBoolean("return"));
        assertEquals(10, model.findGameByName("tata").getPlayers().get(1).getCards().size());
        assertEquals("azert1", model.findGameByName("tata").getActualPlayer().getName());
        assertEquals(1, model.findGameByName("tata").getCptDrawCard());
    }

    @Test
    public void jouerUnPlus2EtJoueurSuivantJouePlus2EtJoueurDapresPioche4Cartes() throws JSONException{
        assertTrue(model.findGameByName("tata").start());

        // Ajout des cartes aux joueurs
        model.findGameByName("tata").getPlayers().get(0).getCards().add(new Card(Value.DrawTwo, Color.Red));
        model.findGameByName("tata").getPlayers().get(1).getCards().add(new Card(Value.DrawTwo, Color.Blue));

        // Ajout d'un couleur rouge au stack
        model.findGameByName("tata").getStack().addCard(new Card(Value.Five, Color.Red));
        model.findGameByName("tata").setActualColor(Color.Red);
        assertEquals(8, model.findGameByName("tata").getPlayers().get(1).getCards().size());


        JSONObject jsonEntity = new JSONObject();
        jsonEntity.put("value", "DrawTwo");
        jsonEntity.put("color", "Red");
        Response response = target("/game/tata/toto").request().header("token", "token").put(Entity.entity(jsonEntity.toString(), MediaType.APPLICATION_JSON));

        assertEquals(200, response.getStatus());
        // Parse la reponse en JSON
        JSONObject json = new JSONObject(response.readEntity(String.class));
        assertTrue(json.getBoolean("success"));
        assertEquals("Red", model.findGameByName("tata").getActualColor().toString());
        assertEquals("azert0", model.findGameByName("tata").getActualPlayer().getName());

        jsonEntity.put("value", "DrawTwo");
        jsonEntity.put("color", "Blue");

        response = target("/game/tata/azert0").request().header("token", "token0").put(Entity.entity(jsonEntity.toString(), MediaType.APPLICATION_JSON));

        json = new JSONObject(response.readEntity(String.class));
        assertTrue(json.getBoolean("success"));
        assertEquals("Blue", model.findGameByName("tata").getActualColor().toString());
        assertEquals("azert1", model.findGameByName("tata").getActualPlayer().getName());

        response = target("/game/tata/azert1").request().header("token", "token1").post(Entity.entity(jsonEntity.toString(), MediaType.APPLICATION_JSON));
        JSONObject jsonObject = new JSONObject(response.readEntity(String.class));

        assertTrue(jsonObject.getBoolean("return"));
        assertEquals(11, model.findGameByName("tata").getPlayers().get(2).getCards().size());
        assertEquals("azert2", model.findGameByName("tata").getActualPlayer().getName());
        assertEquals(1, model.findGameByName("tata").getCptDrawCard());
    }
}

