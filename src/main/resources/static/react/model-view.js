function DomainModelView(props) {
    const [rows, setRows] = React.useState([]);
    const [loading, setLoading] = React.useState(true);
    const [error, setError] = React.useState("");

    React.useEffect(() => {
        setLoading(true);
        fetch(props.endpoint)
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Request failed: " + response.status);
                }
                return response.json();
            })
            .then((data) => {
                setRows(data);
                setError("");
            })
            .catch((e) => setError(e.message))
            .finally(() => setLoading(false));
    }, [props.endpoint]);

    return React.createElement(
        "div",
        null,
        React.createElement("h1", null, props.title),
        loading && React.createElement("p", null, "Loading..."),
        error && React.createElement("p", { style: { color: "red" } }, error),
        !loading && !error && React.createElement(
            "table",
            { border: "1", cellPadding: "8", style: { borderCollapse: "collapse", width: "100%" } },
            React.createElement(
                "thead",
                null,
                React.createElement(
                    "tr",
                    null,
                    props.columns.map((column) => React.createElement("th", { key: column.key }, column.label))
                )
            ),
            React.createElement(
                "tbody",
                null,
                rows.map((row, index) => React.createElement(
                    "tr",
                    { key: row.id ?? index },
                    props.columns.map((column) => React.createElement("td", { key: column.key }, String(row[column.key] ?? "")))
                ))
            )
        )
    );
}

function mountDomainModelView(config) {
    const root = ReactDOM.createRoot(document.getElementById("root"));
    root.render(React.createElement(DomainModelView, config));
}
