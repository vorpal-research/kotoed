import {CodeAnnotation, ReviewAnnotations} from "../state/annotations";
import {Map} from "immutable";
import {sendAsync} from "../../views/components/common";
import {Kotoed} from "../../util/kotoed-api";
import Address = Kotoed.Address;

interface AnnotationResponse {
    map: [[string, CodeAnnotation[]]]
}

export async function fetchAnnotations(submissionId: number): Promise<ReviewAnnotations> {

    let annotations =
        await sendAsync(Address.Api.Submission.Annotations, {id: submissionId}) as AnnotationResponse;

    return Map(annotations.map)
}
