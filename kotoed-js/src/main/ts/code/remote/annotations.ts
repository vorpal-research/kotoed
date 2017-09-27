import {ReviewAnnotations} from "../state/annotations";
import {Map} from "immutable";

export async function fetchAnnotations(submissionId: number): Promise<ReviewAnnotations> {
    return Map({
        "test/lesson2/task1/Tests.kt": [{
            message: "Да у тебя тут бага, чувак!",
            severity: "warning" as "warning",
            position: {
                line: 12,
                col: 12
            }
        },{
            message: "Да у тебя тут ваще бага, чувак!",
            severity: "error" as "error",
            position: {
                line: 12,
                col: 10
            }
        }]
    })
}