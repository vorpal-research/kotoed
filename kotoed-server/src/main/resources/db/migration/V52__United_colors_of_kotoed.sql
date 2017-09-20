-- Student failure
UPDATE tag SET style = '{"backgroundColor": "red"}'::jsonb
    WHERE name in ('invalid', 'build failed', 'tests failed', 'empty', 'waiting for fix', 'bad style');

-- Requires teachers' attention
UPDATE tag SET style = '{"backgroundColor": "red", "color": "gold"}'::jsonb
    WHERE name in ('to be investigated', 'check me');

-- Grades
UPDATE tag SET style = '{"backgroundColor": "white", "color": "green", "border": "1px solid green"}'::jsonb
    WHERE name = 'excellent';
UPDATE tag SET style = '{"backgroundColor": "white", "color": "blue", "border": "1px solid blue"}'::jsonb
    WHERE name = 'good';
UPDATE tag SET style = '{"backgroundColor": "white", "color": "darkviolet", "border": "1px solid darkviolet"}'::jsonb
    WHERE name = 'mediocre';
UPDATE tag SET style = '{"backgroundColor": "white", "color": "red", "border": "1px solid red"}'::jsonb
    WHERE name = 'fair';

-- Build OK
UPDATE tag SET style = '{"backgroundColor": "lightgreen"}'::jsonb
    WHERE name in ('build ok');


-- The skull!
UPDATE tag SET style = '{"backgroundColor": "black"}'::jsonb
    WHERE name = '☠';

-- Teachers
UPDATE tag SET style = '{"backgroundColor": "brown"}'::jsonb
    WHERE name = 'AA';
UPDATE tag SET style = '{"backgroundColor": "orange"}'::jsonb
    WHERE name = 'BS';
UPDATE tag SET style = '{"backgroundColor": "indigo"}'::jsonb
    WHERE name = 'EK';
UPDATE tag SET style = '{"backgroundColor": "pink"}'::jsonb
    WHERE name = 'IE';
UPDATE tag SET style = '{"backgroundColor": "green"}'::jsonb
    WHERE name = 'KG';
UPDATE tag SET style = '{"backgroundColor": "magenta"}'::jsonb
    WHERE name = 'MA';
UPDATE tag SET style = '{"backgroundColor": "white", "color": "black", "border": "1px solid black"}'::jsonb
    WHERE name = 'MB';
UPDATE tag SET style = '{"backgroundColor": "gold"}'::jsonb
    WHERE name = 'MG';
UPDATE tag SET style = '{"backgroundColor": "#008ffe"}'::jsonb
    WHERE name = 'MP';
