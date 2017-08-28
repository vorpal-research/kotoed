CREATE FUNCTION insert_god() RETURNS INT AS '
  INSERT INTO role(name) VALUES (''god'') RETURNING id
' LANGUAGE SQL;

CREATE FUNCTION insert_teacher() RETURNS INT AS '
  INSERT INTO permission(name) VALUES (''teacher'') RETURNING id
' LANGUAGE SQL;

INSERT INTO role_permission(role_id, permission_id)
  VALUES(
    insert_god(),
    insert_teacher()
  );
