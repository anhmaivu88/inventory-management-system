package solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import problem.Matrix;
import problem.ProblemSpec;
import problem.Simulator;
import problem.Store;

public class RTDP {
	
	private ProblemSpec spec = new ProblemSpec();
	private Store store;
    private List<Matrix> probabilities;
    private Simulator simulator;
    	
	public RTDP(ProblemSpec spec) {
		this.spec = spec;
		store = spec.getStore();
        probabilities = spec.getProbabilities();
        simulator = new Simulator(spec, false);
	}
	
	public List<Integer> selectAction(List<Integer> state, int timeLimit) {
		List<Integer> action = new ArrayList<Integer>();
		double qValue = Double.NEGATIVE_INFINITY;
		
		double startTime = System.currentTimeMillis();

		while (System.currentTimeMillis() - startTime < timeLimit) {
			List<Integer> order = new ArrayList<Integer>();
			order = generateAction(state);
			double q = qValue(state, order);
			if (q > qValue) {
				qValue = q;
				action.clear();
				action.addAll(order);
			}
		}

		return action;
	}
	
	public List<Integer> selectLargeAction(List<Integer> state, int timeLimit) {
		List<Integer> action = new ArrayList<Integer>();
		double qValue = Double.NEGATIVE_INFINITY;
		
		double startTime = System.currentTimeMillis();

		while (System.currentTimeMillis() - startTime < timeLimit) {
			List<Integer> order = new ArrayList<Integer>();
			order = generateRandomAction(state);
			double q = qValue(state, order);
			if (q > qValue) {
				qValue = q;
				action.clear();
				action.addAll(order);
			}
		}

		return action;
	}
	
	private double qValue(List<Integer> state, List<Integer> action) {
		double immediateReward = reward(state);
		double expectedReward = reward(state, action);
		double transition = transition(state, action);
		
		return immediateReward + (spec.getDiscountFactor() * (transition * expectedReward));
	}
	
	public List<Integer> generateAction(List<Integer> state) {
		List<Integer> itemOrders = new ArrayList<Integer>();
		List<Integer> itemReturns = new ArrayList<Integer>();
		
		int totalItems = state.stream().mapToInt(Integer::intValue).sum();
		int totalOrders = 0;
		int totalReturns = 0;
		
		Random random = new Random();
		
		for (int i = 0; i < store.getMaxTypes(); i++) {
			if (totalItems >= store.getCapacity() || totalOrders >= store.getMaxPurchase()) {
				itemOrders.add(0);
			} else {
				int orders = random.nextInt((store.getMaxPurchase() + 1) - totalOrders);
				while (totalItems + orders > store.getCapacity()) {
					orders = random.nextInt((store.getMaxPurchase() + 1) - totalOrders);
				}
				itemOrders.add(orders);
				totalOrders += orders;
				totalItems += orders;
			}
			if (totalReturns >= store.getMaxReturns()) {
				itemReturns.add(0);
			} else {
				if (state.get(i) == 0) {
					itemReturns.add(0);
				} else {
					int returns = random.nextInt((store.getMaxReturns() + 1) - totalReturns);
					itemReturns.add(returns);
					totalReturns += returns;
				}
			}
		}
						
		List<Integer> action = new ArrayList<Integer>(itemOrders.size());
		for(int i = 0; i < itemOrders.size(); i++) {
			if (state.get(i) + itemOrders.get(i) - itemReturns.get(i) < 0) {
				action.add(itemOrders.get(i));
			} else {
				action.add(itemOrders.get(i) - itemReturns.get(i));
			}
		}
		
		return itemOrders;
	}
	
	public List<Integer> generateRandomAction(List<Integer> state) {
		List<Integer> action = new ArrayList<Integer>();
		Random random = new Random();
		
		int totalItems = 0;
		int totalOrder = 0;
		for (int i : state) {
			totalItems += i;
		}
		
		while (totalOrder < store.getMaxPurchase()) {
			action.clear();
			totalItems -= totalOrder;
			totalOrder = 0;
			for (int i = 0; i < store.getMaxTypes(); i++) {
				if (totalItems >= store.getCapacity() ||
				    totalOrder >= store.getMaxPurchase()) {
					action.add(0);
				} else {
					int order = random.nextInt((1 - 0) + 1) + 0;
					action.add(order);
					if (order > 0) {
						totalOrder++;
						totalItems++;
					}
				}
			}
		}

		return action;
	}
	
	private double transition(List<Integer> state, List<Integer> action) {
		double totalTransitionProbability = 1.0;
		List<Integer> nextState = nextState(state, action);
		List<Integer> userWants = simulator.sampleUserWants(state);
		
		for (int i = 0; i < nextState.size(); i++) {
			nextState.set(i, nextState.get(i) - userWants.get(i));
			if (nextState.get(i) < 0) {
				nextState.set(i, 0);
			}
		}
		
		for (int i = 0; i < store.getMaxTypes(); i++) {
			double transitionProbability = 0.0;
			int totalItems = state.get(i) + action.get(i);
			if (nextState.get(i) > totalItems) {
				transitionProbability = 0.0;
			} else if (nextState.get(i) > 0 && nextState.get(i) <= totalItems) {
				transitionProbability = probabilities.get(i).get(totalItems, totalItems - nextState.get(i));
			} else if (nextState.get(i) == 0) {
				for (int j = totalItems; j < store.getCapacity(); j++) {
					transitionProbability += probabilities.get(i).get(totalItems, j);
				}
			}
			if (transitionProbability == 0.0) {
				transitionProbability = 1.0;
			}
			totalTransitionProbability *= transitionProbability;
		}
		
		return totalTransitionProbability;
	}
	
	private double reward(List<Integer> state) {
		double totalReward = 0.0;
		
		for (int i = 0; i < store.getMaxTypes(); i++) {
			double reward = 0.0;
			for (int j = state.get(i) + 1; j < store.getCapacity(); j++) {
				reward = (j - state.get(i)) * spec.getPrices().get(i) * (probabilities.get(i).get(state.get(i), j));
			}
			totalReward += -1 * reward;
		}
		
		return totalReward;
	}
	
	private double reward(List<Integer> state, List<Integer> action) {
		double totalReward = 0.0;
		
		for (int i = 0; i < store.getMaxTypes(); i++) {
			double reward = 0.0;
			for (int j = state.get(i) + action.get(i) + 1; j < store.getCapacity(); j++) {
				reward = (j - state.get(i) - action.get(i)) * spec.getPrices().get(i)
					   * (probabilities.get(i).get(state.get(i) + action.get(i), j));
			}
			totalReward += -1 * reward;
		}
		
		return totalReward;
	}
	
	private List<Integer> nextState(List<Integer> state, List<Integer> action) {
		List<Integer> nextState = new ArrayList<Integer>();
		
		for (int i = 0; i < store.getMaxTypes(); i++) {
			nextState.add(state.get(i) + action.get(i));
		}
		
		return nextState;
	}
}
