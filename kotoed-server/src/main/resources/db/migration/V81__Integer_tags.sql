ALTER TABLE TAG ADD deprecated BOOLEAN DEFAULT FALSE NOT NULL;

UPDATE tag
SET deprecated = true
WHERE name ~ E'^(-)?[0-9]+\.[0-9]+$';

UPDATE tag
SET style = '{"color": "#333333", "backgroundColor": "white"}'
WHERE tag.deprecated = true;

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-1', '{"color": "#ff0000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-2', '{"color": "#ee0000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-3', '{"color": "#e30000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-4', '{"color": "#e90000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-5', '{"color": "#dd0000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-6', '{"color": "#cf0000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-7', '{"color": "#d30000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-8', '{"color": "#d60000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-9', '{"color": "#da0000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-10', '{"color": "#cc0000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-12', '{"color": "#bb0000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-14', '{"color": "#aa0000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-16', '{"color": "#990000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-18', '{"color": "#880000", "backgroundColor": "white"}');

INSERT INTO tag (id, name, style)
VALUES (DEFAULT, '-20', '{"color": "#770000", "backgroundColor": "white"}');