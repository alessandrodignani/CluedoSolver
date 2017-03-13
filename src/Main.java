import aima.core.logic.fol.inference.InferenceResult;
import aima.core.logic.fol.kb.FOLKnowledgeBase;
import aima.core.logic.fol.parsing.*;
import aima.core.logic.fol.domain.*;
import aima.core.logic.fol.parsing.ast.Sentence;

import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by Alessandro Dignani
 * dignani.alessandro@gmail.com
 * on 06/05/16.
 */

public class Main {
    private static FOLDomain cluedoDomain = new FOLDomain();
    private static FOLParser parser = new FOLParser(cluedoDomain);
    private static FOLKnowledgeBase  kb = new FOLKnowledgeBase(cluedoDomain);
    public static final int numOfPlayers = getNumberOfPlayers();
    public static int myPlayer;
    public static ArrayList<Card> deck = new ArrayList<>();

    public static void main(String[] args){

        SetupKB(numOfPlayers);

        myPlayer = getPlayerNumber(numOfPlayers);

        insertCards(myPlayer);

        startGame();
    }

    private static void startGame() {
        ArrayList<Card> hypothesis = new ArrayList<>(3);
        int currentPlayer = 0;
        int currentReplyer;
        boolean stop;

        while(true){
            System.out.println("Player P" + currentPlayer + "'s turn");
            System.out.println("Makes hypothesis? (1/0)");
            Scanner in = new Scanner(System.in);
            int makeHypothesis = in.nextInt();

            if(makeHypothesis==1){
                hypothesis = getCurrentHypothesis();
                currentReplyer = getNextPlayer(currentPlayer);

                do{
                    stop = doesThePlayerHasSomething(currentReplyer);
                    if(stop){
                        manageStopped(currentReplyer, hypothesis);
                        if(currentPlayer==myPlayer){
                            manageReply(currentReplyer);
                        }
                        break;
                    }
                    else {
                        manageContinued(currentReplyer, hypothesis);
                    }
                    checkKB(currentReplyer);

                    currentReplyer = getNextPlayer(currentReplyer);
                }while(currentReplyer != currentPlayer);

                checkKB(currentReplyer);
            }

            currentPlayer = getNextPlayer(currentPlayer);
            System.out.println("\nKnowledge Base:\n" + kb.toString());
        }
    }

    private static void manageReply(int currentReplyer) {
        String str;
        Scanner in = new Scanner(System.in);
        System.out.println("What card did he show you?");
        str = in.nextLine();
        addKnowledge(currentReplyer, new Card(str));
    }

    private static void checkKB(int player) {
        for(Card card : deck){
            Sentence query = parser.parse("Has(P" + player + ", " + card.description + ")");
            InferenceResult answer = kb.ask(query);
            if (answer.isTrue()){
                if(!kb.getOriginalSentences().contains(query)){
                    kb.tell(query);
                    System.out.println("INSERTED NEW KNOWLEDGE: " + query.toString());
                }
            }

            // Then control if it's in the solution
            query = parser.parse("Has(SOLUTION, " + card.description + ")");
            answer = kb.ask(query);
            if (answer.isTrue()){
                if(!kb.getOriginalSentences().contains(query)){
                    kb.tell(query);
                    System.out.println("INSERTED NEW KNOWLEDGE: " + query.toString());
                }
            }
        }
    }

    private static void manageStopped(int currentReplyer, ArrayList<Card> hypothesis) {
        Sentence s = parser.parse("((Has(P" + currentReplyer + ", " + hypothesis.get(0).description + ") OR Has(P" + currentReplyer + ", " + hypothesis.get(1).description + ")) OR Has(P" + currentReplyer + ", " + hypothesis.get(2).description + "))");
        if(!kb.ask(s).isTrue())
            kb.tell(s);
    }

    private static void manageContinued(int currentReplyer, ArrayList<Card> hypothesis) {
        for (Card card : hypothesis) {
            addKnowledgeNot(currentReplyer, card);
        }
    }

    private static boolean doesThePlayerHasSomething(int currentReplyer) {
        Scanner in = new Scanner(System.in);
        System.out.println("Does player P" + currentReplyer + " has something? (1/0)");
        int choice = in.nextInt();
        if (choice==1)
            return true;
        else
            return false;
    }

    private static int getNextPlayer(int currentPlayer) {
        if(currentPlayer == numOfPlayers-1)
            return 0;
        else
            return currentPlayer+1;
    }

    private static void insertCards(int player) {
        int choice;
        Scanner in = new Scanner(System.in);
        do{
            System.out.println("Do you want to insert a new card? (1/0)");
            choice = in.nextInt();
            if(choice==1){
                System.out.println("Insert the card:");
                String card = in.next();
                addKnowledge(player, new Card(card));
            }
        }
        while(choice==1);

        // Insert knowledge on cards that I don't have
        Sentence s;
        for (Card card : deck){
            s = parser.parse("Has(P" + player + ", " + card.description + ")");
            if(!kb.ask(s).isTrue())
                addKnowledgeNot(player, card);
        }
    }

    public static void addKnowledge(int player, Card card){
        Sentence s = parser.parse("Has(P" + player + ", " + card.description + ")");
        if(!kb.ask(s).isTrue())
            kb.tell(s);
            System.out.println("INSERTED NEW KNOWLEDGE: " + s.toString());
        for(int i=0; i<numOfPlayers; i++){
            if(i!=player){
                addKnowledgeNot(i, card);
            }
        }
        notInSolution(card);
    }

    private static void notInSolution(Card card) {
        Sentence s = parser.parse("NOT(Has(SOLUTION, " + card.description + "))");
        if(!kb.ask(s).isTrue())
            kb.tell(s);
    }

    public static void addKnowledgeNot(int player, Card card){
        Sentence s = parser.parse("NOT(Has(P" + player + ", " + card.description + "))");
        if(!kb.ask(s).isTrue())
            kb.tell(s);
    }

    private static int getNumberOfPlayers() {
        int n;
        Scanner in = new Scanner(System.in);
        do{
            System.out.println("How many players? (2 to 6)");
            n = in.nextInt();
        }
        while(n < 2 || n > 6);
        System.out.println("There are " + n + " players");
        return n;
    }

    private static int getPlayerNumber(int numOfPlayers) {
        int me;
        Scanner in = new Scanner(System.in);
        do{
            System.out.println("What player are you? (0 to " + (numOfPlayers - 1) + ")");
            me = in.nextInt();
        }
        while(me < 0 || me >= numOfPlayers);
        System.out.println("You are player P" + me);
        return me;
    }

    private static void SetupKB(int numOfPlayers){
        // Rooms
        for (Room r : Room.values()) {
            cluedoDomain.addConstant(r.toString());
            deck.add(new Card(r.toString()));
        }

        // Characters
        for (Character c : Character.values()) {
            cluedoDomain.addConstant(c.toString());
            deck.add(new Card(c.toString()));
        }

        // Weapons
        for (Weapon w : Weapon.values()) {
            cluedoDomain.addConstant(w.toString());
            deck.add(new Card(w.toString()));
        }

        // Players
        for(int i = 0; i < numOfPlayers; i++)
            cluedoDomain.addConstant("P"+i);

        cluedoDomain.addConstant("SOLUTION");

        cluedoDomain.addPredicate("Has");

        // Someone must have each card (either players or solution)
        for(Card card : deck){
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i<numOfPlayers; i++){
                sb.append("(");
            }

            sb.append("Has(SOLUTION, " + card.description + ")");

            for(int i = 0; i<numOfPlayers; i++){
                sb.append(" OR Has(P" + i + ", " + card.description + "))");
            }

            Sentence s = parser.parse(sb.toString());
            kb.tell(s);
        }
    }

    public static ArrayList<Card> getCurrentHypothesis() {
        ArrayList<Card> currentHypothesis = new ArrayList<>();

        Scanner in = new Scanner(System.in);
        String s;

        System.out.println("Which Room?");
        s = in.next();
        currentHypothesis.add(new Card(s));

        System.out.println("Who?");
        s = in.next();
        currentHypothesis.add(new Card(s));

        System.out.println("Which Weapon?");
        s = in.next();
        currentHypothesis.add(new Card(s));

        return currentHypothesis;
    }
}
