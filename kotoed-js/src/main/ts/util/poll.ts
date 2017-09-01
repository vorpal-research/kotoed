import {sleep} from "./common";

export abstract class PollingStrategy {
    private readonly shouldGiveUpExt: () => boolean;
    constructor(shouldGiveUp: () => boolean = () => false) {
        this.shouldGiveUpExt = shouldGiveUp;
    }
    /**
     * Should we give up even if we already have good enough result?
     * If your polling strategy is based on states based on iterations,
     * then you should NOT update this state here because it's not guaranteed
     * to be called exactly once per iteration of polling.
     */
    shouldAbruptlyGiveUp(): boolean {
        return this.shouldGiveUpExt();
    }

    /**
     * Should we give up even if we don't have good enough result?
     *
     * If your polling strategy is based on states based on iterations,
     * then you should NOT update this state here because it's not guaranteed
     * to be called exactly once per iteration of polling.
     */
    shouldGiveUp(): boolean {
        return this.shouldAbruptlyGiveUp();
    }

    /**
     * Wait till next iteration.
     *
     * This method is called EXACTLY ONCE PER ITERATION, so it's a good place to update your iteration-based state.
     */
    abstract async wait(): Promise<void>
}

interface SimplePollingStrategyParams {
    interval?: number,
    shouldGiveUp?: () => boolean
}

export class SimplePollingStrategy extends PollingStrategy {
    private readonly interval: number;
    constructor({interval = 1000, shouldGiveUp = () => false}: SimplePollingStrategyParams) {
        super(shouldGiveUp);
        this.interval = interval;
    }

    async wait() {
        await sleep(this.interval)
    }
}

interface DespairingPollingStrategyParams {
    despairFactor?: number,
    baseInterval?: number,
    despairDivider?: number,
    maxDespairDegree?: number,
    shouldGiveUp?: () => boolean
}

export class DespairingPollingStrategy extends PollingStrategy {
    private readonly despairFactor: number;
    private readonly baseInterval: number;
    private readonly despairDivider: number;
    private readonly maxDespairDegree: number;
    private currentIteration: number = 0;
    private currentDespairDegree: number = 0;

    constructor(
        {
            despairFactor = 2,
            baseInterval = 1000,
            despairDivider = 10,
            maxDespairDegree = 6, // 2**6 = 64s,
            shouldGiveUp = () => false
        }: DespairingPollingStrategyParams) {
        super(shouldGiveUp);
        this.despairFactor = despairFactor;
        this.baseInterval = baseInterval;
        this.despairDivider = despairDivider;
        this.maxDespairDegree = maxDespairDegree;
    }


    shouldGiveUp(): boolean {
        return this.currentDespairDegree > this.maxDespairDegree || this.shouldAbruptlyGiveUp();
    }

    async wait(): Promise<void> {
        await sleep(this.baseInterval * Math.pow(this.despairFactor, this.currentDespairDegree));
        this.currentIteration++;
        if (this.currentIteration == this.despairDivider) {
            this.currentIteration = 0;
            this.currentDespairDegree++;
        }
    }
}

export async function poll<T>(
    {
        action,  // This action is performed AT LEAST ONCE, so we have at least one result from it.
        isGoodEnough = () => true,
        onIntermediate = () => {},
        beforeAction = () => {},
        onFinal = () => {},
        onGiveUp = () => {},
        strategy = new SimplePollingStrategy({})
    } : {
        action: () => Promise<T>,
        isGoodEnough?: (res: T) => boolean,
        beforeAction?: () => void,
        onIntermediate?: (intermediate: T) => void,
        onFinal?: (final: T) => void,
        onGiveUp?: (last: T) => void,
        strategy?: PollingStrategy
    }) {
    let gaveUp = false;
    let intermediate: T;
    let iter = 0;
    while (true) {
        beforeAction();
        // Action is always performed at least once. And we have at least one intermediate result.
        intermediate = await action();

        // Does someone still care about the result?
        if (strategy.shouldAbruptlyGiveUp()) {
            gaveUp = true;
            break;
        }
        // Is it good enough?
        if (isGoodEnough(intermediate))
            break;

        // Should we try again?
        if (strategy.shouldGiveUp()) {
            gaveUp = true;
            break;
        }
        // We won't call onIntermediate on our last result
        onIntermediate(intermediate);
        await strategy.wait();
        iter++;
    }
    if (!gaveUp) {
        onFinal(intermediate);
    } else {
        onGiveUp(intermediate);
    }
}

export async function pollDespairing<T>(
        {
            action,
            isGoodEnough = () => true,
            onIntermediate = () => {},
            onFinal = () => {},
            onGiveUp = () => {},
            strategyParams = {}
        } : {
            action: () => Promise<T>,
            isGoodEnough?: (res: T) => boolean,
            onIntermediate?: (intermediate: T) => void,
            onFinal?: (final: T) => void,
            onGiveUp?: (last: T) => void,
            strategyParams?: DespairingPollingStrategyParams
        }) {
    await poll({
        action, isGoodEnough, onIntermediate, onFinal, onGiveUp,
        strategy: new DespairingPollingStrategy(strategyParams)
    })
}