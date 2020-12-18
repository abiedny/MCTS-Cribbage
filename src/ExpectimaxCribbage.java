import java.io.Console;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ExpectimaxCribbage implements CribbagePlayer {

    private int player;
    private int dets;
    private int fuckups = 0;
    private int normals = 0;

    /**
     * Cribbage AI player that uses expectimax to choose a move.
     * Takes n determinizations, and solves each with minimax.
     * @param dets Number of determinizations made
     * @throws Exception
     */
    public ExpectimaxCribbage(int dets) throws Exception {
        this.dets = dets;
    }

    /**
     * Uses two player minimax from sturtevant
     * @param state
     * @return
     * @throws Exception
     */
    public int search(CribbageState state) throws Exception {
        this.player = state.playerToMove();

        ArrayList<Integer> actions = new ArrayList<Integer>();
        for (int i = 0; i < dets; i++) {
            // Take a determinization, everything random except known values
            CribbageState determ = new CribbageState(state);
            determ.randomize(player);

            // Now make a minimax tree for that determinization. We build the tree until the
            // hand is over, cloning the
            // state every time so that each branch is its own state with an action applied
            Node parent = new Node(determ, -1, null, NodeType.MAX, player);
            parent.buildChildren(parent);

            // Then search it with minimax and return the resultant action int
            Node best = minimax(parent);
            // I only care about the first action taken, so move up until we get that
            while (best.getParent().getParent() != null) {
                best = parent;
            }
            int action = best.getAction();
            int[] moves = state.getActions(); // check the action against the real state, mostly for debug
            boolean illegal = true;
            for (int j = 0; j < moves.length; j++) {
                if (action == moves[j]) {
                    illegal = false;
                    break;
                }
            }
            if (illegal) {
                // something fucky happened
                fuckups++;
                action = moves[0];
            } else {
                normals++;
            }
            System.out.println("Fuckups: " + fuckups);
            System.out.println("Normals: " + normals);
            actions.add(action);
        }
        // if any action was selected more than others, do it
        int bestAction = 0;
        int bestFreq = 0;
        for (int action : actions) {
            int freq = Collections.frequency(actions, action);
            if (freq > bestFreq) {
                bestFreq = freq;
                bestAction = action;
            }
        }
        return bestAction;
    }

    private Node minimax(Node node) {
        if (node.getChildren().size() == 0) return node;

        int currentBest = 0;
        Node currentBestNode = null;
        if (node.type == NodeType.MIN) {
            currentBest = Integer.MAX_VALUE;
            for (Node child : node.getChildren()) {
                Node childSearch = minimax(child);
                if (childSearch.getReward() < currentBest) {
                    currentBestNode = childSearch;
                    currentBest = childSearch.getReward();
                }
            }
        }
        else if (node.type == NodeType.MAX) {
            currentBest = Integer.MIN_VALUE;
            for (Node child : node.getChildren()) {
                Node childSearch = minimax(child);
                if (childSearch.getReward() > currentBest) {
                    currentBestNode = childSearch;
                    currentBest = childSearch.getReward();
                }
            }

        }
        return currentBestNode;
    }

    @Override
    public int getMove(CribbageState gameState) throws Exception {
        int[] moves = gameState.getActions();
		if (moves.length == 1) {
			return moves[0];
		}
		else {
			return search(gameState);
		}
    }
    
    public String toString() {
        return "Expectimax, " + dets + " determinizations";
    }

    private enum NodeType {
        MIN,
        MAX
    }

    private class Node {
        private CribbageState state;
        private int action;
        private Node parent;
        private ArrayList<Node> children;
        private NodeType type;
        private int player;

        public Node(CribbageState state, int action, Node parent, NodeType type, int player) {
            this.state = state;
            this.action = action;
            this.parent = parent;
            this.type = type;
            this.player = player;
            this.children = new ArrayList<Node>();
        }

        public void buildChildren(Node parent) throws Exception {
            //build out the children until the hand is over

            for (int action : parent.getState().getActions()) {
                //each node needs a new state so that we aren't applying actions horizontally across nodes
                CribbageState s = new CribbageState(parent.getState());
                s = s.applyAction(action);

                boolean handIsOver = false;
                if (s.playerToMove() == -1) {
                    //check if the hand is over
                    handIsOver = s.handOver();

                    //this means the game has to move (to deal or shit) so just pass it up
                    int[] actions = s.getActions();
                    s = s.applyAction(actions[0]);
                }

                boolean swapType = s.playerToMove() != parent.getState().playerToMove(); //player to move changes
                NodeType newType = swapType ? ( (parent.getType() == NodeType.MAX) ? NodeType.MIN : NodeType.MAX ) : parent.getType(); //so we change the node type;

                Node newNode = new Node(s, action, this, newType, s.playerToMove());
                parent.getChildren().add(newNode);

                if (!s.handOver() && !handIsOver) {
                    //hand isn't over in this state so we keep building children
                    buildChildren(newNode);
                }
            }
        }

        public ArrayList<Node> getChildren() {
            return this.children;
        }

        public int getReward() {
            return state.getHandPointDiff(this.player);
        }

        public CribbageState getState() {
            return this.state;
        }

        public int getAction() {
            return this.action;
        }

        public Node getParent() {
            return this.parent;
        }

        public NodeType getType() {
            return this.type;
        }
    }
}
