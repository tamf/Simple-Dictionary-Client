import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.System;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

//
// This is an implementation of a simplified version of a command 
// line dictionary client. The program takes no arguments.
//

public class CSdict {
	static final int MAX_LEN = 255;
	static final int PERMITTED_ARGUMENT_COUNT = 1;
	static Boolean debugOn = false;

	// Socket connection fields
	static Socket dictSocket;
	static PrintWriter out;
	static BufferedReader in;

	// Status of the program
	static String status = "DISCONNECTED";
	static boolean validAndExpectedCommand = true;

	// The current dictionary in use
	static String currDict = "*";

	public static void main(String[] args) {
		byte cmdString[] = new byte[MAX_LEN];

		if (args.length == PERMITTED_ARGUMENT_COUNT) {
			debugOn = args[0].equals("-d");
			if (debugOn) {
				System.out.println("Debugging output enabled");
			} else {
				System.out.println("997 Invalid command line option - Only -d is allowed");
				return;
			}
		} else if (args.length > PERMITTED_ARGUMENT_COUNT) {
			System.out.println("996 Too many command line options - Only -d is allowed");
			return;
		}

		try {
			for (int len = 1; len > 0;) {
				flushByteArray(cmdString);
				System.out.print("csdict> ");
				len = System.in.read(cmdString);
				if (len <= 0)
					break;
				// Start processing the command here.
				String cmd = new String(cmdString).trim();

				// Ignore empty line, #
				if (cmd.isEmpty() || cmd.substring(0, 1).equals("#")) {
					continue;
				}

				// Quit on Ctrl-D
				if (cmd.equals("^D")) {
					exit(new String[0]);
				}

				processCmd(cmd);
				// Do not process response if command is invalid or unexpected
				if (validAndExpectedCommand)
					processResponse(cmd);
				validAndExpectedCommand = true;
			}
		} catch (SocketException e) {
			System.err.println("925 Control connection I/O error, closing control connection.");
			closeConnections(new String[0]);
		} catch (IOException exception) {
			System.err.println("998 Input error while reading commands, terminating.");
			exit(new String[0]);
		}
	}

	private static void flushByteArray(byte[] bytes) {
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = 0;
		}
	}

	/**
	 * Establish a socket and I/O stream connection to server on portNumber
	 *
	 * @param params
	 *            the parameter list that should contain exactly the server and
	 *            portNumber to connect to
	 */
	private static void establishConnection(String[] params) {
		if (params.length == 0 || params.length > 2) {
			System.err.println("901 Incorrect number of arguments.");
			validAndExpectedCommand = false;
			return;
		}

		String server = null;
		int portNumber = 2628;

		try {
			server = params[0];
			portNumber = params.length == 1 ? portNumber : Integer.parseInt(params[1]);

			if (debugOn) {
				// print OPEN command for debug
				System.out.println("--> OPEN " + server + " " + portNumber);
			}

			dictSocket = new Socket();
			dictSocket.connect(new InetSocketAddress(server, portNumber), 30000);
			out = new PrintWriter(dictSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(dictSocket.getInputStream()));

			// Update status to CONNECTED
			status = "CONNECTED";
		} catch (SocketTimeoutException e) {
			// Connection attempt timed out or socket not created
			System.err.println("920 Control connection to " + server + " on port " + portNumber + " failed to open.");
			validAndExpectedCommand = false;
		} catch (IOException e) {
			// Invalid server or other connection errors
			System.err.println("920 Control connection to " + server + " on port " + portNumber + " failed to open.");
			validAndExpectedCommand = false;
		} catch (IllegalArgumentException e) {
			// Invalid port #
			System.err.println("902 Invalid argument.");
			validAndExpectedCommand = false;
		}
	}

	/**
	 * Close the current socket and I/O stream connections
	 *
	 * @param params
	 *            the parameter list that should be empty
	 */
	private static void closeConnections(String[] params) {
		if (params.length != 0) {
			System.err.println("901 Incorrect number of arguments.");
			validAndExpectedCommand = false;
			return;
		}

		try {
			// Notify server of connection termination

			out.println("quit");
			// print out command and response before closing connection
			if (debugOn) {
				System.out.println("--> quit");
				System.out.println("<-- " + in.readLine());
			}

			// Close socket & I/O streams
			out.close();
			in.close();
			dictSocket.close();
		} catch (IOException e) {
			System.err.println("925 Control connection I/O error, closing control connection.");
		} finally {
			// Update status to DISCONNECTED
			status = "DISCONNECTED";
		}
	}

	/**
	 * Closes any open socket and I/O connections and cleanly exits the program
	 *
	 * @param params
	 *            the parameter list that should be empty
	 */
	private static void exit(String[] params) {
		if (params.length != 0) {
			System.err.println("901 Incorrect number of arguments.");
			validAndExpectedCommand = false;
			return;
		}

		// Close socket & I/O connections first if currently connected
		if (status.equals("CONNECTED")) {
			closeConnections(params);
		}
		System.exit(0);
	}

	/**
	 * Get the list of dictionaries this server supports
	 *
	 * @param params
	 *            the parameter list that should be empty
	 */
	private static void getSupportedDicts(String[] params) {
		if (params.length != 0) {
			System.err.println("901 Incorrect number of arguments.");
			validAndExpectedCommand = false;
			return;
		}

		// Ping server for supported dictionaries
		if (debugOn) {
			System.out.println("--> show db");
		}
		out.println("show db");
	}

	/**
	 * Set the currDict to use
	 *
	 * @param params
	 *            the currDict to set to
	 */
	private static void setCurrDict(String[] params) {
		if (params.length != 1) {
			System.err.println("901 Incorrect number of arguments.");
			validAndExpectedCommand = false;
			return;
		}

		// Update the currDict
		currDict = params[0];
	}

	/**
	 * Gets & prints the currDict in use
	 *
	 * @param params
	 *            the parameter list that should be empty
	 */
	private static void getCurrDict(String[] params) {
		if (params.length != 0) {
			System.err.println("901 Incorrect number of arguments.");
			validAndExpectedCommand = false;
			return;
		}

		// Print the currDict
		System.out.println(currDict);
	}

	/**
	 * Gets & prints the definitions
	 * 
	 * @param params
	 */
	private static void define(String[] params) {
		if (params.length != 1) {
			System.err.println("901 Incorrect number of arguments.");
			validAndExpectedCommand = false;
			return;
		}

		// Send command DEFINE currDict word
		String command = "DEFINE " + currDict + " " + params[0];
		if (debugOn) {
			// Print DEFINE command
			System.out.println("--> " + command);
		}
		out.println(command);

	}

	/**
	 * Gets & prints any matches found using server-dependent default strategy
	 * against all databases
	 */
	private static void matchDefault(String word) {
		if (word == null) {
			System.err.println("902 Invalid argument");
			return;
		}

		String command = "MATCH * . " + word;
		if (debugOn) {
			System.out.println("--> " + command);
		}
		out.println(command);
	}

	/**
	 * Gets & prints any exact matches found
	 * 
	 * @param params
	 */
	private static void match(String[] params) {
		if (params.length != 1) {
			System.err.println("901 Incorrect number of arguments.");
			validAndExpectedCommand = false;
			return;
		}

		// Send command MATCH currDict exact word
		String command = "MATCH " + currDict + " exact " + params[0];
		if (debugOn) {
			System.out.println("--> " + command);
		}
		out.println(command);

	}

	/**
	 * Gets & prints any prefix matches found
	 * 
	 * @param params
	 */
	private static void prefixmatch(String[] params) {
		if (params.length != 1) {
			System.err.println("901 Incorrect number of arguments.");
			validAndExpectedCommand = false;
			return;
		}

		// Send command MATCH currDict prefix word
		String command = "MATCH " + currDict + " prefix " + params[0];
		if (debugOn) {
			System.out.println("--> " + command);
		}
		out.println(command);
	}

	/**
	 * Check whether the given cmd is an accepted command
	 *
	 * @param cmd
	 *            the command to check
	 * @return true if cmd is valid, false otherwise
	 */
	private static boolean isCmdValid(String cmd) {
		boolean valid = false;
		switch (cmd.toLowerCase()) {
		case "open":
		case "dict":
		case "set":
		case "currdict":
		case "define":
		case "match":
		case "prefixmatch":
		case "close":
		case "quit":
			valid = true;
			break;
		default:
			System.err.println("900 Invalid command.");
			break;
		}
		return valid;
	}

	/**
	 * Check whether the given cmd is expected at this time
	 *
	 * @param cmd
	 *            the command to check
	 * @return true if the cmd is expected at this time, false otherwise
	 */
	private static boolean isCmdExpected(String cmd) {
		boolean expected = false;
		if (status.equals("CONNECTED")) {
			switch (cmd.toLowerCase()) {
			case "dict":
			case "set":
			case "currdict":
			case "define":
			case "match":
			case "prefixmatch":
			case "close":
			case "quit":
				expected = true;
				break;
			default:
				System.err.println("903 Supplied command not expected at this time.");
				break;
			}
		} else if (status.equals("DISCONNECTED")) {
			switch (cmd.toLowerCase()) {
			case "open":
			case "quit":
				expected = true;
				break;
			default:
				System.err.println("903 Supplied command not expected at this time.");
				break;
			}
		}
		return expected;
	}

	/**
	 * Process the cmd, stripping out cmd and parameters
	 *
	 * @param cmd
	 *            the command string to be processed
	 */
	private static void processCmd(String cmd) {
		// Split the cmd string by whitespace into cmd and params
		String[] splitCmd = cmd.split("\\s+");
		cmd = splitCmd[0];
		String[] params = Arrays.copyOfRange(splitCmd, 1, splitCmd.length);

		// Check if cmd is valid & expected at this time
		if (!isCmdValid(cmd) || !isCmdExpected(cmd)) {
			validAndExpectedCommand = false;
			return;
		}

		switch (cmd.toLowerCase()) {
		case "open":
			// Establish socket connections
			establishConnection(params);
			break;
		case "dict":
			// Get list of supported dictionaries
			getSupportedDicts(params);
			break;
		case "set":
			// Set the currDict
			setCurrDict(params);
			break;
		case "currdict":
			// Print the currDict
			getCurrDict(params);
			break;
		case "define":
			// Get definitions
			define(params);
			break;
		case "match":
			// Get exact matches
			match(params);
			break;
		case "prefixmatch":
			// Get prefix matches
			prefixmatch(params);
			break;
		case "close":
			// Print out response, Close socket, I/O connections
			closeConnections(params);
			break;
		case "quit":
			// Close socket, I/O connections & exit program
			exit(params);
			break;
		default:
			System.err.println("900 Invalid command.");
			break;
		}
	}

	/**
	 * Identifies response code
	 * 
	 * @param str
	 * @return true if str is a response code, otherwise false
	 */
	private static boolean isResponseCode(String str) {
		return str.matches("\\d{3}");

	}

	/**
	 * Gets & prints the definitions.
	 * 
	 * @param word
	 * 
	 * @throws IOException
	 */
	private static void processDefineResponse(String word) throws IOException {
		String fromServer;
		String prevLine = "";
		int numDefs = 1;
		boolean errorEncountered = false;

		while ((fromServer = in.readLine()) != null) {
			String[] splitResponseLine = fromServer.split("\\s+");
			String firstWord = splitResponseLine.length > 0 ? splitResponseLine[0] : "";
			if (isResponseCode(firstWord)) {
				if (debugOn) {
					System.out.println("<-- " + fromServer);
				}

				switch (firstWord) {
				case "552":
					System.out.println("***No definition found***");

					// send MATCH command to find word
					matchDefault(word);

					// process response from MATCH command
					processMatchResponse("***No dictionaries have a definition for this word***");

					errorEncountered = true;
					break;
				case "150":
					// Set the number of definitions found
					numDefs = Integer.parseInt(splitResponseLine[1]);
					break;
				case "151":
					// Print header for each definition -- database name - text
					// follows
					StringBuilder header = new StringBuilder("@");
					for (int i = 2; i < splitResponseLine.length; i++) {
						header.append(' ');
						header.append(splitResponseLine[i]);
					}
					System.out.println(header);
					break;
				case "550":
					System.err.println("930 Dictionary does not exist");
					errorEncountered = true;
					break;
				case "250":
					break;
				default:
					break;
				}

			} else {
				System.out.println(fromServer);
			}
			if (prevLine.equals(".")) {
				numDefs--;
			}
			// Stop reading when no more definitions or if we've hit an error
			if (numDefs <= 0 || errorEncountered) {
				break;
			}
			prevLine = fromServer;
		}
	}

	/**
	 * Prints line to the console, but only if line is not a response code or
	 * debug is on.
	 * 
	 * @param line
	 */
	private static void printLineExceptResponseCodeUnlessDebug(String line) {
		if (line == null)
			return;
		String[] splitResponseLine = line.split("\\s+");
		String firstWord = splitResponseLine[0];

		if (!isResponseCode(firstWord)) {
			System.out.println(line);
			return;
		}
		if (debugOn) {
			System.out.println("<-- " + line);
		}
	}

	/**
	 * Gets & prints exact matches from server.
	 * 
	 * @throws IOException
	 */
	private static void processMatchResponse() throws IOException {
		processMatchResponse("****No matching word(s) found****");
	}

	/**
	 * Gets & prints prefix matches from server.
	 * 
	 * @throws IOException
	 */
	private static void processPrefixMatchResponse() throws IOException {
		processMatchResponse("*****No prefix matches found*****");
	}

	/**
	 * Gets & prints matches from server. Prints nothingFoundMessage to the
	 * console if no matches found.
	 * 
	 * @param nothingFoundMessage
	 * @throws IOException
	 */
	private static void processMatchResponse(String nothingFoundMessage) throws IOException {
		String fromServer;
		String prevLine = "";
		boolean errorEncountered = false;
		while ((fromServer = in.readLine()) != null) {
			String[] splitResponseLine = fromServer.split("\\s+");
			String firstWord = splitResponseLine[0];

			if (isResponseCode(firstWord)) {
				if (debugOn) {
					System.out.println("<-- " + fromServer);
				}

				switch (firstWord) {
				case "552":
					System.out.println(nothingFoundMessage);
					errorEncountered = true;
					break;
				case "550":
					System.err.println("930 Dictionary does not exist");
					errorEncountered = true;
					break;
				case "152":
					break;
				case "250":
					break;
				default:
					break;
				}

			} else {
				System.out.println(fromServer);
			}

			if (prevLine.equals(".") || errorEncountered) {
				break;
			}
			prevLine = fromServer;
		}
	}

	private static void processDictResponse() throws IOException {
		String fromServer;
		String prevLine = "";

		while ((fromServer = in.readLine()) != null) {
			String[] splitResponseLine = fromServer.split("\\s+");
			String firstWord = splitResponseLine[0];

			printLineExceptResponseCodeUnlessDebug(fromServer);

			// if we get 554, it means no DB present
			if (firstWord.equals("554"))
				break;

			if (prevLine.equals("."))
				break;

			prevLine = fromServer;
		}
	}

	private static void processOpenResponse() throws IOException {
		String fromServer;

		fromServer = in.readLine();
		if (fromServer != null) {
			String[] splitResponseLine = fromServer.split("\\s+");
			String firstWord = splitResponseLine[0];

			printLineExceptResponseCodeUnlessDebug(fromServer);

			switch (firstWord) {
			case "220":
				break;
			default:
				System.err
						.println("999 Processing error. Server may not be running a DICT server. Closing connection.");
				closeConnections(new String[0]);
				break;
			}
		}

	}

	/**
	 * Processes the responses from the server
	 * 
	 * @param cmd
	 * @throws IOException
	 */
	private static void processResponse(String cmd) throws IOException {
		String[] splitCmd = cmd.split("\\s+");
		cmd = splitCmd[0];

		switch (cmd.toLowerCase()) {
		case "open":
			processOpenResponse();
			break;
		case "dict":
			processDictResponse();
			break;
		case "define":
			// process response from DEFINE command, passing in the word
			// parameter
			processDefineResponse(splitCmd[1]);
			break;
		case "match":
			processMatchResponse();
			break;
		case "prefixmatch":
			processPrefixMatchResponse();
			break;
		default:
			break;
		}
	}
}
