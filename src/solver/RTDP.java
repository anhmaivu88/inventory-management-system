package solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import problem.Matrix;
import problem.ProblemSpec;
import problem.Store;

public class RTDP {
	
	private ProblemSpec spec = new ProblemSpec();
	private Store store;
    private List<Matrix> probabilities;
	
	public RTDP(ProblemSpec spec) {
		this.spec = spec;
		store = spec.getStore();
        probabilities = spec.getProbabilities();
	}
	
	public List<Integer> selectAction(List<Integer> stockInventory) {
		List<Integer> itemOrders = new ArrayList<Integer>();
		double qValue = Double.NEGATIVE_INFINITY;
		
		double startTime = System.currentTimeMillis();

		while (System.currentTimeMillis() - startTime < 1000) {
			List<Integer> orders = generateAction(stockInventory);
			double q = qValue(stockInventory, orders);
			if (q > qValue) {
				qValue = q;
				itemOrders.clear();
				itemOrders.addAll(orders);
			}
		}

		return itemOrders;
	}
	
	private double qValue(List<Integer> state, List<Integer> action) {
		double immediateReward = reward(state);
		double expectedReward = reward(state, action);
		double transition = transition(state, action);
		
		return immediateReward + (spec.getDiscountFactor() * (transition * expectedReward));
	}
	
	private List<Integer> generateAction(List<Integer> state) {
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
				int returns = random.nextInt((store.getMaxReturns() + 1) - totalReturns);
				itemReturns.add(returns);
				totalReturns += returns;
			}
		}
						
		List<Integer> action = new ArrayList<Integer>(itemOrders.size());
		for(int i = 0; i < itemOrders.size(); i++) {
			if (itemOrders.get(i) - itemReturns.get(i) < 0) {
				action.add(0);
			} else {
				action.add(itemOrders.get(i) - itemReturns.get(i));
			}
		}
		
		return action;
	}
	
	private double transition(List<Integer> state, List<Integer> action) {
		double totalTransitionProbability = 1.0;
		List<Integer> nextState = nextState(state, action);
		
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
			totalTransitionProbability *= transitionProbability;
		}
		
		return totalTransitionProbability;
	}
	
	private double reward(List<Integer> state) {
		double totalReward = 0.0;
		
		for (int i = 0; i < store.getMaxTypes(); i++) {
			double reward = 0.0;
			for (int j = state.get(i) + 1; j < store.getCapacity(); j++) {
				reward = j - state.get(i) * (probabilities.get(i).get(state.get(i), j));
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
				reward = j - state.get(i) - action.get(i) 
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
