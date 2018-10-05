declare module "twemoji" {

    interface TwemojiOptions {
        callback?: (s: string) => string,
        attributes?: (icon: string, variant: string) => string,
        base?: string,
        ext?: string,
        className?: string,
        size?: string|number,
        folder?: string
    }

    public interface Twemoji {
        parse(x: any, options?: TwemojiOptions): string;
    }

    declare const twemoji: Twemoji;
    export default twemoji;
}