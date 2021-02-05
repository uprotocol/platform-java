package org.monora.uprotocol.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.communication.ContentException;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.protocol.communication.UndefinedErrorCodeException;
import org.monora.uprotocol.core.protocol.communication.client.UnauthorizedClientException;
import org.monora.uprotocol.core.protocol.communication.client.UntrustedClientException;
import org.monora.uprotocol.core.spec.v1.Keyword;

import java.io.IOException;

/**
 * Handle reading and writing of a response for the protocol
 */
public class Responses
{
    /**
     * Checks the errors in a response.
     * <p>
     * This will throw the appropriate error when the response includes an error.
     *
     * @param client     That we are receiving the response from.
     * @param jsonObject The JSON data to read from.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException If the response has an error description
     */
    public static void checkError(Client client, JSONObject jsonObject) throws JSONException, ProtocolException
    {
        if (jsonObject.has(Keyword.ERROR)) {
            final String errorCode = jsonObject.getString(Keyword.ERROR);
            switch (errorCode) {
                case Keyword.ERROR_NOT_ALLOWED:
                    throw new UnauthorizedClientException(client);
                case Keyword.ERROR_NOT_TRUSTED:
                    throw new UntrustedClientException(client);
                case Keyword.ERROR_NOT_ACCESSIBLE:
                    throw new ContentException(ContentException.Error.NotAccessible);
                case Keyword.ERROR_ALREADY_EXISTS:
                    throw new ContentException(ContentException.Error.AlreadyExists);
                case Keyword.ERROR_NOT_FOUND:
                    throw new ContentException(ContentException.Error.NotFound);
                case Keyword.ERROR_UNKNOWN:
                    throw new ProtocolException();
                default:
                    throw new UndefinedErrorCodeException(errorCode);
            }
        }
    }

    /**
     * Find the appropriate error code of the protocol for the given exception.
     *
     * @param exception The error for which the appropriate error string will be generated.
     * @return The error code that represents the given exception.
     * @throws ProtocolException If the error is undefined.
     */
    public static String getError(Exception exception) throws ProtocolException
    {
        try {
            throw exception;
        } catch (UntrustedClientException e) {
            return Keyword.ERROR_NOT_TRUSTED;
        } catch (UnauthorizedClientException e) {
            return Keyword.ERROR_NOT_ALLOWED;
        } catch (PersistenceException e) {
            return Keyword.ERROR_NOT_FOUND;
        } catch (ContentException e) {
            switch (e.error) {
                case NotFound:
                    return Keyword.ERROR_NOT_FOUND;
                case NotAccessible:
                    return Keyword.ERROR_NOT_ACCESSIBLE;
                case AlreadyExists:
                    return Keyword.ERROR_ALREADY_EXISTS;
                default:
                    return Keyword.ERROR_UNKNOWN;
            }
        } catch (Exception e) {
            throw new ProtocolException("Could not handle the undefined exception.", e);
        }
    }

    /**
     * Check the result in a response.
     * <p>
     * This will throw {@link JSONException} if the object does not contain a result.
     *
     * @param jsonObject The JSON data to read from.
     * @return True if the result is positive.
     * @throws JSONException If the object does not contain a result.
     */
    public static boolean getResult(JSONObject jsonObject) throws JSONException
    {
        return jsonObject.getBoolean(Keyword.RESULT);
    }

    /**
     * Insert the error code into the given JSON object.
     *
     * @param jsonObject To insert into.
     * @param errorCode  To insert.
     * @throws JSONException If something goes wrong when inserting into the JSON object.
     */
    public static void insertError(JSONObject jsonObject, String errorCode) throws JSONException
    {
        jsonObject.put(Keyword.ERROR, errorCode);
    }

    /**
     * Insert the appropriate protocol error inside the JSON object according to the exception given.
     *
     * @param jsonObject To insert into.
     * @param exception  With which this will decide which error code to insert.
     * @throws JSONException     If something goes wrong when inserting into the JSON object.
     * @throws ProtocolException With the cause exception if the error is not known.
     */
    public static void insertError(JSONObject jsonObject, Exception exception) throws JSONException, ProtocolException
    {
        insertError(jsonObject, getError(exception));
    }

    /**
     * Insert a result into a JSON object.
     *
     * @param jsonObject To insert into.
     * @param result     To insert.
     * @throws JSONException If inserting the result into the JSON object fails.
     * @see #receiveResult(ActiveConnection, Client)
     */
    public static void insertResult(JSONObject jsonObject, boolean result) throws JSONException
    {
        jsonObject.put(Keyword.RESULT, result);
    }

    /**
     * Receive a response from remote and validate it.
     * <p>
     * This will throw the appropriate error when something is not right.
     * <p>
     * The error messages are sent using {@link #send}.
     *
     * @param activeConnection The active connection instance.
     * @param client           That we are receiving the response from.
     * @return The JSON data that doesn't seem to contain an error.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public static JSONObject receiveChecked(ActiveConnection activeConnection, Client client) throws IOException,
            JSONException, ProtocolException
    {
        JSONObject jsonObject = activeConnection.receive().getAsJson();
        checkError(client, jsonObject);
        return jsonObject;
    }

    /**
     * Receive and validate a response. If it doesn't contain an error, get the result.
     *
     * @param activeConnection The active connection instance.
     * @param client           That we are receiving the response from.
     * @return True if the result is positive.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public static boolean receiveResult(ActiveConnection activeConnection, Client client) throws IOException,
            JSONException, ProtocolException
    {
        return getResult(receiveChecked(activeConnection, client));
    }

    /**
     * Send a JSON data that includes the result.
     *
     * @param activeConnection The active connection instance.
     * @param result           If the result is successful.
     * @param jsonObject       To send along with the result.
     * @throws IOException   If an IO error occurs.
     * @throws JSONException If something goes wrong when creating JSON object.
     * @see #receiveResult(ActiveConnection, Client)
     */
    public static void send(ActiveConnection activeConnection, boolean result, JSONObject jsonObject)
            throws JSONException, IOException
    {
        insertResult(jsonObject, result);
        activeConnection.reply(jsonObject);
    }

    /**
     * Send an error to remote.
     *
     * @param activeConnection The active connection instance.
     * @param exception        With which this will decide which error code to send.
     * @param jsonObject       The send with the error.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException With the cause exception if the error is undefined.
     * @see #receiveChecked(ActiveConnection, Client)
     */
    public static void send(ActiveConnection activeConnection, Exception exception, JSONObject jsonObject)
            throws IOException, JSONException, ProtocolException
    {
        send(activeConnection, getError(exception), jsonObject);
    }


    /**
     * Send an error to remote.
     *
     * @param activeConnection The active connection instance.
     * @param errorCode        To send.
     * @param jsonObject       To send along with the error.
     * @throws IOException   If an IO error occurs.
     * @throws JSONException If something goes wrong when creating JSON object.
     * @see #receiveChecked(ActiveConnection, Client)
     */
    public static void send(ActiveConnection activeConnection, String errorCode, JSONObject jsonObject)
            throws IOException, JSONException
    {
        insertError(jsonObject, errorCode);
        send(activeConnection, false, jsonObject);
    }
}
