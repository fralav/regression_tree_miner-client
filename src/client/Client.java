package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;

import utility.Keyboard;

/**
 * La classe {@code Client} esegue l'interfaccia utente e permette la comunicazione con il server.
 * @author Francesco Lavecchia
 */
public class Client {
	/**
	 * Rappresenta l'ID del task che preleva i nomi delle tabelle dal database.
	 */
    private static final int TASK_GET_TABLES_FROM_DB = 1;
    
    /**
     * Rappresenta l'ID del task che preleva i nomi dei file dall'archivio.
     */
    private static final int TASK_GET_FILES_FROM_ARCHIVE = 2;
    
    /**
     * Rappresenta l'ID del task che apprende un albero da un training set del database.
     */
    private static final int TASK_LEARN_TREE_FROM_DB = 3;
    
    /**
     * Rappresenta l'ID del task che preleva un albero precedentemente serializzato su file.
     */
    private static final int TASK_GET_TREE_FROM_FILE = 4;
    
    /**
     * Rappresenta l'ID del task che fornisce al client la rappresentazione in {@link String} dell'albero.
     */
    private static final int TASK_PRINT_TREE = 5;
    
    /**
     * Rappresenta l'ID del task che fornisce la predizione dell'albero al client.
     */
    private static final int TASK_PREDICT_TREE = 6;

    /**
     * Viene inviato al client quando un operazione va a buon fine.
     */
    private static final String OK = "ok";
    
    /**
     * Viene inviato al client quando si verifica un errore in {@code Data}.
     */
    private static final String DATA_ERROR = "dataError";
    
    /**
     * Viene inviato al client quando non ci sono tabelle nel database.
     */
    private static final String TABLE_NOT_FOUND = "tableNotFound";
    
    /**
     * Viene inviato al client quando non ci sono file nell'archivio.
     */
    private static final String FILE_NOT_FOUND = "fileNotFound";
    
    /**
     * Viene inviato al client quando la tabella selezionata non è presenta nel database.
     */
    private static final String NO_TABLES_FOUND = "NoTablesFound";
    
    /**
     * Viene inviato al client quando il file selezionato non è presente nell'archivio.
     */
    private static final String NO_FILES_FOUND = "NoFilesFound";

    /**
     * Rappresenta lo stream in input del socket.
     */
    private static ObjectInputStream in ;
    
    /**
     * Rappresenta lo stream in output del socket.
     */
    private static ObjectOutputStream out;

    /**
     * Il metodo costruttore instaura la connessione con il server tramite socket.
     * @param ip Indirizzo IP del server.
     * @param port Porta del server.
     * @throws IOException Viene lanciato quando si verifica un errore con la comunicazione del server.
     */
    public Client(String ip, int port) throws IOException {
        InetAddress address = InetAddress.getByName(ip);
        Socket socket = new Socket(address, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    /** Il metodo implementa l'interfaccia grafica a riga di comando la quale rende possibile la comunicazione tra l'utente e il server.
     * Una volta instaurata la connessione, l'utente può scegliere se avviare un nuovo processo di regression tree o recuperare albero precedentemente
     * serializzato su file.
     * @param args Indirizzo del server e porta sono acquisiti come parametri tramite args.
     */
    public static void main(String[] args) {
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        Client main = null;
        char answer = 'Y';
        String tableName = "";
        
        // Verifica connessione al server
        try {
            main = new Client(ip, port);
            System.out.println("Connesso al server " + args[0] + ":" + args[1]);
        } catch (IOException e) {
            System.out.println("[!] Connessione fallita.");
            return;
        }
        
        // Finché il client è connesso...
        do {
        	
            // Decidi db o file
            int decision;
            do {
                System.out.println("\n[1] Apprendi l'albero dal database.");
                System.out.println("[2] Carica l'albero dall'archivio.");
                decision = Keyboard.readInt();
            } while (!(decision == 1) && !(decision == 2));
            
            // se scelta db
            if (decision == 1) {
                LinkedList <String> tableList = new LinkedList <String> ();
                try {
                    tableList = getTablesFromDb();
                    if (tableList.toString().equals(NO_TABLES_FOUND)) {
                        System.out.println("[!] Non ci sono tabelle nel database.");
                        continue;
                    } else {
                        System.out.println("\nScegli una delle tabelle elecante qui sotto:");
                        System.out.println(tableList);
                    }
                } catch (ClassNotFoundException | IOException e) {
                    System.out.println("[!] Si è verificato un errore durante la connessione al server.");
                    e.printStackTrace();
                    return;
                }
                do {
                    System.out.println("\nScegli il nome della tabella:");
                    tableName = Keyboard.readString();
                } while (!tableList.contains(tableName));

                System.out.println("\nInizio della fase di acquisizione...");
                try {
                    String report = learnTreeFromDb(tableName);
                    if (report.equals(OK)) {
                        System.out.println("Training set appreso con successo.");
                    } else if (report.equals(DATA_ERROR)) {
                        System.out.println("[!] Si è verificato un errore durante l'elaborazione del training set.");
                        return;
                    } else if (report.equals(TABLE_NOT_FOUND)) {
                        System.out.println("[!] La tabella specificata non è stata trovata.");
                        continue;
                    }
                } catch (ClassNotFoundException | IOException e) {
                    System.out.println("[!] Si è verificato un errore durante la connessione al server.");
                    e.printStackTrace();
                    return;
                }

            // se scelta file
            } else if (decision == 2) {
                LinkedList <String> fileList = new LinkedList <String>();
                try {
                    fileList = getFilesFromArchive();
                    if (fileList.contains(NO_FILES_FOUND)) {
                        System.out.println("[!] Non ci sono file nell'archivio.");
                        continue;
                    } else {
                        System.out.println("\nScegli uno dei file elecanti qui sotto:");
                        System.out.println(fileList);
                    }
                } catch (ClassNotFoundException | IOException e) {
                    System.out.println("[!] Si è verificato un errore durante la connessione al server.");
                    e.printStackTrace();
                    return;
                }
                do {
                    System.out.println("\nScegli il nome del file:");
                    tableName = Keyboard.readString();
                } while (!fileList.contains(tableName));

                System.out.println("\nInizio della fase di acquisizione...");

                try {
                    String report = getTreeFromFile(tableName);
                    if (report.equals(OK)) {
                        System.out.println("Albero acquisito con successo.");
                    } else if (report.equals(FILE_NOT_FOUND)) {
                        System.out.println("[!] Il file specificato non è stato trovato.");
                        continue;
                    }
                } catch (ClassNotFoundException | IOException e) {
                    System.out.println("[!] Si è verificato un errore durante la connessione al server.");
                    e.printStackTrace();
                    return;
                }
            }

            decision = 0;
            do {
                System.out.println("\n[1] Stampa l'albero di regressione.");
                System.out.println("[2] Predici l'albero di regressione.");
                decision = Keyboard.readInt();
            } while (!(decision == 1) && !(decision == 2));

            if (decision == 1) {
                System.out.println("\nAlbero di regressione del training set \'" + tableName + "\'.");
                try {
                    System.out.println(printTree());
                } catch (ClassNotFoundException | IOException e) {
                    System.out.println("[!] Si è verificato un errore durante la connessione al server.");
                    e.printStackTrace();
                    return;
                }
            } else if (decision == 2) {
                System.out.println("\nPredizione dell'albero di regressione del training set \'" + tableName + "\'.");
                try {
                    predictTree();
                } catch (ClassNotFoundException | IOException e) {
                    System.out.println("[!] Si è verificato un errore durante la connessione al server.");
                    e.printStackTrace();
                    return;
                }
            }
            do {
                System.out.println("\n\nVuoi continuare? (y/n)");
                answer = Keyboard.readChar();
            } while (Character.toUpperCase(answer) != 'Y' && Character.toUpperCase(answer) != 'N');
        } while (Character.toUpperCase(answer) == 'Y');
    }
    
    /**
     * Chiede al server i nomi delle tabelle all'interno del database e riceve dal server stesso i risultati.
     * @return Nomi tabelle presenti nel database.
     * @throws IOException Può essere sollevata durante un errore di comunicazione con il server.
     * @throws ClassNotFoundException Può essere sollevata durante la lettura dello stream di input del socket.
     */
    private static LinkedList < String > getTablesFromDb() throws IOException, ClassNotFoundException {
        out.writeObject(TASK_GET_TABLES_FROM_DB);
        return (LinkedList < String > ) in .readObject();
    }

    /**
     * Chiede al server i nomi dei file all'interno dell'archivio e riceve dal server stesso i risultati.
     * @return Nomi file presenti nell'archivio.
     * @throws IOException Può essere sollevata durante un errore di comunicazione con il server.
     * @throws ClassNotFoundException Può essere sollevata durante la lettura dello stream di input del socket.
     */
    private static LinkedList < String > getFilesFromArchive() throws IOException, ClassNotFoundException {
        out.writeObject(TASK_GET_FILES_FROM_ARCHIVE);
        return (LinkedList < String > ) in .readObject();
    }

    /**
     * Chiede al server di apprendere l'albero dal training set all'interno della tabella selezionata in input e riceve l'esito.
     * @param table Nome della tabella del database.
     * @return Esito dell'apprendimento del training set.
     * @throws IOException Può essere sollevata durante un errore di comunicazione con il server.
     * @throws ClassNotFoundException Può essere sollevata durante la lettura dello stream di input del socket.
     */
    private static String learnTreeFromDb(String table) throws IOException, ClassNotFoundException {
        out.writeObject(TASK_LEARN_TREE_FROM_DB);
        out.writeObject(table);
        return (String) in .readObject();
    }

    /**
     * Chiede al server di prelevare l'albero serializzato precedentemente su un file dell'archivio e riceve l'esito.
     * @param file Nome del file dell'archivio.
     * @return Esito del prelievo dell'albero da file.
     * @throws IOException Può essere sollevata durante un errore di comunicazione con il server.
     * @throws ClassNotFoundException Può essere sollevata durante la lettura dello stream di input del socket.
     */
    private static String getTreeFromFile(String file) throws IOException, ClassNotFoundException {
        out.writeObject(TASK_GET_TREE_FROM_FILE);
        out.writeObject(file);
        return (String) in .readObject();
    }

    /**
     * Chiede al server di fornirgli l'albero appreso sottoforma di stringa e riceve l'esito.
     * @return Albero appreso sottoforma di stringa.
     * @throws IOException Può essere sollevata durante un errore di comunicazione con il server.
     * @throws ClassNotFoundException Può essere sollevata durante la lettura dello stream di input del socket.
     */
    private static String printTree() throws IOException, ClassNotFoundException {
        out.writeObject(TASK_PRINT_TREE);
        return (String) in .readObject();
    }

    /**
     * Chiede al server di avviare il procedimento di predizione e, finché il nodo corrente non è un nodo foglia, il server chiede al client di
     * selezionare lo split corrispondente al nodo foglia da predire. Quando il nodo corrente è un nodo foglia, il server fornisce al client
     * il valore di predizione dell'esempio corrispondente.
     * @throws IOException Può essere sollevata durante un errore di comunicazione con il server.
     * @throws ClassNotFoundException Può essere sollevata durante la lettura dello stream di input del socket.
     */
    private static void predictTree() throws IOException, ClassNotFoundException {
        String answer = "";
        int risp = -1;
        int numberChildren;
        out.writeObject(TASK_PREDICT_TREE);
        answer = in .readObject().toString();
        while (answer.equals("QUERY")) {
            answer = in .readObject().toString();
            System.out.println(answer);
            numberChildren = Integer.parseInt( in .readObject().toString());
            do {
                risp = Keyboard.readInt();
            } while (risp < 0 || risp >= numberChildren);
            out.writeObject(risp);
            answer = in .readObject().toString();
        }
        if (answer.equals("OK")) {
            answer = in .readObject().toString();
            System.out.println("Valore di predizione: " + answer + ".");
        }
    }
    
}