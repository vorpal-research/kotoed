import * as React from "react";
import {render} from "react-dom";
import {SubmissionToRead} from "../data/submission";
import SubmissionDetails from "./components/SubmissionDetails";

const items: Array<SubmissionToRead> = [
    {
        id: 1,
        datetime: 0,
        parentSubmissionId: 2,
        projectId: 42,
        state: "open",
        revision: "co5fefe"
    },
    {
        id: 2,
        datetime: 0,
        parentSubmissionId: 3,
        projectId: 42,
        state: "open",
        revision: "co5fefe"
    },
    {
        id: 3,
        datetime: 0,
        parentSubmissionId: 4,
        projectId: 42,
        state: "open",
        revision: "co5fefe"
    },
    {
        id: 4,
        datetime: 0,
        parentSubmissionId: 5,
        projectId: 42,
        state: "open",
        revision: "co5fefe"
    },
    {
        id: 5,
        datetime: 0,
        parentSubmissionId: 6,
        projectId: 42,
        state: "open",
        revision: "co5fefe"
    }
];

render(
    <SubmissionDetails
        submission={{
            record: {
                id: 42,
                parentSubmissionId: 43,
                revision: "covfefe",
                state: "closed",
                datetime: 0,
                projectId: 42
            },
            verificationData: {
                status: "Invalid"
            }
        }}
        history={{
            onMore: () => console.log("More clicked"), items: items
        }}
        permissions={{
            resubmit: true,
            changeState: true
        }}
        comments={{
            open: 42,
            closed: 25
        }}
        onResubmit={() => console.log("Resubmit")}
        onReopen={() => console.log("Reopen")}
        onClose={() => console.log("Close")}
        onMount={() => {}}
    />,
    document.getElementById("submission-details-app"));
