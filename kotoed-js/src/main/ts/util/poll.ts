import {sleep} from "./common";

export abstract class PollingStrategy {
    abstract shouldGiveUp(): boolean
    abstract async wait(): Promise<void>
}

export class SimplePollingStrategy extends PollingStrategy {
    private readonly interval: number;
    private readonly shouldGiveUpExt: () => boolean;
    constructor(interval: number, shouldGiveUp: () => boolean = () => false) {
        super();
        this.interval = interval;
        this.shouldGiveUpExt = shouldGiveUp;
    }

    shouldGiveUp() {
        return this.shouldGiveUpExt();
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
    private readonly shouldGiveUpExt: () => boolean;
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
        super();
        this.despairFactor = despairFactor;
        this.baseInterval = baseInterval;
        this.despairDivider = despairDivider;
        this.maxDespairDegree = maxDespairDegree;
        this.shouldGiveUpExt = shouldGiveUp;
    }


    shouldGiveUp(): boolean {
        return this.currentDespairDegree > this.maxDespairDegree || this.shouldGiveUpExt();
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
        action,
        isGoodEnough = () => true,
        onIntermediate = () => {},
        beforeAction = () => {},
        onFinal = () => {},
        onGiveUp = () => {},
        strategy = new SimplePollingStrategy(1000)
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
        intermediate = await action();
        if (strategy.shouldGiveUp()) {
            gaveUp = true;
            break;
        }
        if (isGoodEnough(intermediate))
            break;

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