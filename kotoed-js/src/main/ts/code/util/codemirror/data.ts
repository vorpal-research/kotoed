import {Map} from "immutable"
import {CmMode} from "./index";

const MODE_KOTLIN: CmMode = {
    mode: "clike",
    contentType: "text/x-kotlin"
};
const MODE_JAVA: CmMode = {
    mode: "clike",
    contentType: "text/x-java"
};
const MODE_SCALA: CmMode = {
    mode: "clike",
    contentType: "text/x-scala"
};

const MODE_LESS: CmMode = {
    mode: "css",
    contentType: "text/x-css"
};
const MODE_CSS: CmMode = {
    mode: "css",
    contentType: "text/x-css"
};
const MODE_TS: CmMode = {
    mode: "javascript",
    contentType: "text/typescript"
};
const MODE_JS: CmMode = {
    mode: "javascript",
    contentType: "text/javascript"
};
const MODE_TSX: CmMode = {
    mode: "jsx",
    contentType: "text/typescript-jsx"
};
const MODE_JSX: CmMode = {
    mode: "jsx",
    contentType: "text/jsx"
};
const MODE_YAML: CmMode = {
    mode: "yaml",
    contentType: "text/x-yaml"
};
const MODE_PYTHON: CmMode = {
    mode: "python",
    contentType: "text/x-python"
};
const MODE_SQL: CmMode = {
    mode: "sql",
    contentType: "text/x-sql"
};
const MODE_JSON: CmMode = {
    mode: "javascript",
    contentType: "application/json"
};
const MODE_PUG: CmMode = {
    mode: "pug",
    contentType: "text/x-pug"
};

const MODE_XML: CmMode = {
    mode: "xml",
    contentType: "text/xml"
};

const MODE_HTML: CmMode = {
    mode: "xml",
    contentType: "text/xml"
};



export const CM_MODES_BY_EXT: Map<string, CmMode> = Map([
    ["kt", MODE_KOTLIN],
    ["kotlin", MODE_KOTLIN],
    ["java", MODE_JAVA],
    ["scala", MODE_SCALA],
    ["less", MODE_LESS],
    ["css", MODE_CSS],
    ["ts", MODE_TS],
    ["typescript", MODE_TS],
    ["js", MODE_JS],
    ["javascript", MODE_JS],
    ["tsx", MODE_TSX],
    ["typescript-jsx", MODE_TSX],
    ["jsx", MODE_JSX],
    ["yml", MODE_YAML],
    ["yaml", MODE_YAML],
    ["py", MODE_PYTHON],
    ["python", MODE_PYTHON],
    ["sql", MODE_SQL],
    ["json", MODE_JSON],
    ["pug", MODE_PUG],
    ["jade", MODE_PUG],
    ["xml", MODE_XML],
    ["html", MODE_HTML],
    ["htm", MODE_HTML],


]);