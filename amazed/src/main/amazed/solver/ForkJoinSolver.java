package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.*;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.Stack;
import java.util.Collections;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver
    extends SequentialSolver
{
    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze)
    {
        super(maze);
        
    }

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal, forking after a given number of visited
     * nodes.
     *
     * @param maze        the maze to be searched
     * @param forkAfter   the number of steps (visited nodes) after
     *                    which a parallel task is forked; if
     *                    <code>forkAfter &lt;= 0</code> the solver never
     *                    forks new tasks
     */
    public ForkJoinSolver(Maze maze, int forkAfter)
    {
    	 this(maze);
         this.forkAfter = forkAfter;
         visited = new ConurrentSkipListSet<>();
         predecessor = new ConcurrentHashMap<>()
    }
    
    public ForkJoinSolver(ForkJoinSolver parent, int start)
    {
        super(maze);
        this.start = start;
        this.visited = parent.visited;
        this.predecessor = parent.predecessor;
        
    }
   

    private int player;
    
    /**
     * The maze being searched.
     */
    protected Maze maze;

    private static boolean notFinished = true;
    
    private ArrayList<ForkJoinSolver> childThreads;
    
   
    /**
     * Searches for and returns the path, as a list of node
     * identifiers, that goes from the start node to a goal node in
     * the maze. If such a path cannot be found (because there are no
     * goals, or all goals are unreacheable), the method returns
     * <code>null</code>.
     *
     * @return   the list of node identifiers from the start node to a
     *           goal node in the maze; <code>null</code> if such a path cannot
     *           be found.
     */             
    @Override
    public List<Integer> compute()
    {
        return parallelSearch();
    }

    private List<Integer> parallelSearch()
    {
    	completedPath = null;
    	childThreads = new ArrayList<>();
        int player = maze.newPlayer(start);
        // start with start node
        frontier.push(start);
        // as long as not all nodes have been processed
        while (notFinished && !frontier.empty()) {
            // get the new node to process
            int current = frontier.pop();
            // if current node has a goal
            if (maze.hasGoal(current)) {
                // move player to goal
            	
                maze.move(player, current);
                // search finished: reconstruct and return path
                completedPath = pathFromTo(start, current);
                notFinished = false;
            }
            
            //Fork after visiting a set amount of nodes
            if (visited.size() % forkAfter == 0) {
            	ForkJoinSolver p1 = new ForkJoinSolver(maze, forkAfter, current);
            	ForkJoinSolver p2 = new ForkJoinSolver(maze, forkAfter, current);
            	p1.fork();
            	p2.compute();
            	p1.join();
            }
            
            // if current node has not been visited yet
            if (!visited.contains(current)) {
                // move player to current node
                maze.move(player, current);
                // mark node as visited
                visited.add(current);
                // for every node nb adjacent to current
                for (int nb: maze.neighbors(current)) {
                	int count=0;
                    // add nb to the nodes to be processed
                    // if nb has not been already visited,
                    // nb can be reached from current (i.e., current is nb's predecessor)
                    if (!visited.contains(nb)){
                    	count+=1;
                    	if count==1{
                    		frontier.push(nb);
                    		predecessor.put(nb, current);
                    	}
                    	else {
                    		predecessor.put(nb, current);
                    		ForkJoinSolver childThread = new ForkJoinSolver(this, start);
                    		childThreads.add(childThread);
                    		childThread.fork();
                    	}
                    }
                }
            }
        }
        
        for (ForkJoinSolver child : childThreads ) {
        	List<Integer> partialPath =f.join();
        	if (partialPath!=null) {
        		completedPath = pathFromTo(start, partialPath.get(0))
        		partialPath.remove(0);
        		completedPath.addAll(partialPath);
        	}
        }
        
        return completedPath;
    }
    
}
