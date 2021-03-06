package fr.unice.idse.services;

import java.util.regex.Pattern;

import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import fr.unice.idse.db.DataBaseUser;
import fr.unice.idse.model.Model;

@Path("auth")
public class AuthRest extends OriginRest{

    /**
     * Authentifie un utilisateur Guest
     * @param stringJson Parametre
     * @return Response
     * @throws JSONException exception
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response authPlayerGuest(String stringJson) throws JSONException {
        Model model = Model.getInstance();
        JSONObject jsonObject = new JSONObject(stringJson);
        JSONObject jsonReturn = new JSONObject();


        if(!jsonObject.has("playername")){
            jsonReturn.put("error", "Missing parameter playername");
            return sendResponse(405, jsonReturn.toString(), "POST");
        }

        // Génération du token
        String token = generateToken(jsonObject.getString("playername"));
        if(model.createPlayer(jsonObject.getString("playername"), token)){
            jsonReturn.put("token", token);
            jsonReturn.put("rang", 2);
            return sendResponse(200, jsonReturn.toString(), "POST");
        }

        jsonReturn.put("error", "Player already exist");
        return sendResponse(405, jsonReturn.toString(), "POST");
    }

    /**
     * Authentifie un utilisateur avec ses identifiants
     * @param json Parametre
     * @return Response
     * @throws JSONException exception
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response authPlayer(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        JSONObject jsonResult = new JSONObject();
        Model model = Model.getInstance();
        DataBaseUser dataBaseUser = new DataBaseUser();
        Pattern pattern = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");

        if(!jsonObject.has("email")){
            jsonResult.put("error", "Missing parameter email");
            return sendResponse(405, jsonResult.toString(), "PUT");
        }

        if(!jsonObject.has("password")){
            jsonResult.put("error", "Missing parameter password");
            return sendResponse(405, jsonResult.toString(), "PUT");
        }

        if(!pattern.matcher(jsonObject.getString("email")).matches()){
            jsonResult.put("error", "Email invalid");
            return sendResponse(405, jsonResult.toString(), "PUT");
        }

        JSONObject jsonDB = dataBaseUser.verifLogin(jsonObject.getString("email"), generatePassword(jsonObject.getString("password")));
        if(jsonDB.length() == 0){
            jsonResult.put("error", "Email or password incorrect");
            return sendResponse(405, jsonResult.toString(), "PUT");
        }

        if(jsonDB.getBoolean("banned")){
            jsonResult.put("error", "Compte banni");
            return sendResponse(405, jsonResult.toString(), "PUT");
        }

        String token = generateToken(jsonObject.getString("email"));
        String playerName = jsonDB.getString("pseudo");
        if(model.createPlayer(playerName, token)){
            jsonResult.put("token", token);
            jsonResult.put("playerName", playerName);
            jsonResult.put("rang", jsonDB.getInt("rang"));
            return sendResponse(200, jsonResult.toString(), "PUT");
        }

        jsonResult.put("error", "Player already exist");
        return sendResponse(405, jsonResult.toString(), "PUT");
    }

    /**
     * Créer un joueur dans la base de donnée
     * @param json Parametre
     * @return Response
     * @throws JSONException exception
     */
    @POST
    @Path("/signup")
    @Produces(MediaType.APPLICATION_JSON)
    public Response signup(String json) throws JSONException {
        // Initialisation des variables
        JSONObject jsonObject = new JSONObject(json);
        JSONObject jsonResult = new JSONObject();
        Model model = Model.getInstance();
        DataBaseUser dataBaseUser = new DataBaseUser();
        Pattern pattern = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");

        // Verification de l'existante des variables
        if(!jsonObject.has("email")){
            jsonResult.put("error", "Missing parameter email");
            return sendResponse(405, jsonResult.toString(), "POST");
        }
        if(!jsonObject.has("playerName")){
            jsonResult.put("error", "Missing parameter playerName");
            return sendResponse(405, jsonResult.toString(), "POST");
        }
        if(!jsonObject.has("password")){
            jsonResult.put("error", "Missing parameter password");
            return sendResponse(405, jsonResult.toString(), "POST");
        }

        // Vérification des variables
        if(!pattern.matcher(jsonObject.getString("email")).matches()){
            jsonResult.put("error", "Email invalid");
            return sendResponse(405, jsonResult.toString(), "POST");
        }
        if(jsonObject.getString("playerName").length() < 3) {
            jsonResult.put("error", "playerName invalid size");
            return sendResponse(405, jsonResult.toString(), "POST");
        }

        // Insertion dans la bdd
        if(!dataBaseUser.addUser(jsonObject.getString("playerName"), jsonObject.getString("email"), generatePassword(jsonObject.getString("password")), 3)){
            jsonResult.put("error", "Player already exist");
            return sendResponse(405, jsonResult.toString(), "POST");
        }

        // Ajout dans le model
        String token = generateToken(jsonObject.getString("playerName"));
        if(model.createPlayer(jsonObject.getString("playerName"), token)){
            jsonResult.put("token", token);
            return sendResponse(200, jsonResult.toString(), "POST");
        }

        jsonResult.put("error", "Player already exist");
        return sendResponse(405, jsonResult.toString(), "POST");
    }

    /**
     * Deconnecte un joueur
     * @param token Token du joueur
     * @return Response
     * @throws JSONException exception
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response signOut(@HeaderParam("token") String token) throws JSONException {
        JSONObject jsonResult = new JSONObject();
        Model model = Model.getInstance();

        if(token == null){
            jsonResult.put("error", "Token miss");
            return sendResponse(405, jsonResult.toString(), "DELETE");
        }

        if(model.getPlayerFromList(token) == null){
            jsonResult.put("error", "Player not exist by token");
            return sendResponse(405, jsonResult.toString(), "DELETE");
        }

        if(model.removePlayer(token)){
            jsonResult.put("status", true);
            return sendResponse(200, jsonResult.toString(), "DELETE");
        }

        jsonResult.put("status", false);
        return sendResponse(405, jsonResult.toString(), "DELETE");
    }

}
