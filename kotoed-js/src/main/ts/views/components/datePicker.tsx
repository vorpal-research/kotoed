import * as moment from "moment";
import 'react-dates/initialize'
import {isInclusivelyBeforeDay, SingleDatePicker} from "react-dates";
import 'react-dates/lib/css/_datepicker.css'
import * as React from "react";

type DatePickerProps = {
    date: moment.Moment,
    onDateChange: (date: moment.Moment) => void
    id ?: string
}

type DatePickerState = {
    focused: boolean
}

export class DatePicker extends React.Component<DatePickerProps, DatePickerState> {
    constructor(props: DatePickerProps) {
        super(props);
        this.state = {
            focused: false
        }
    }

    onFocusChange = (focused: boolean | null) => {
        this.setState({focused : !!focused})
    };

    render() {
        return <SingleDatePicker
            id={this.props.id || "singleDatePickerId"}
            focused={this.state.focused}
            date={this.props.date}
            onFocusChange={(arg) => this.onFocusChange(arg.focused)}
            onDateChange={(moment: moment.Moment | null) => moment && this.props.onDateChange(moment)}
            numberOfMonths={1}
            initialVisibleMonth={() => moment()}
            displayFormat={"DD.MM.YYYY"}
            isDayBlocked={() => false}
            isDayHighlighted={() => false}
            isOutsideRange={(day: any) => !isInclusivelyBeforeDay(day, moment())}
            small
        />
    }
}
