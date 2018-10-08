import * as React from "react";
import {render} from "react-dom";
import {ReportTable} from "./components/reportTable";
import {Kotoed} from "../util/kotoed-api";
import {SingleDatePicker} from "react-dates";
import Address = Kotoed.Address;

let rootElement = document.getElementById("course-report-app")!;

let props = {
    address: Address.Api.Course.Report,
    id: Number(rootElement.getAttribute("data-course-id")),
    timestamp: Number(rootElement.getAttribute("data-timestamp"))
};

render(<ReportTable {...props} />, rootElement);
