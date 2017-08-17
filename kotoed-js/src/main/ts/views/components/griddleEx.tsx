import * as React from "react";

export const ArrayColumn = ({value}: { value: any[] }) =>
    <span>{value.join(", ")}</span>;
