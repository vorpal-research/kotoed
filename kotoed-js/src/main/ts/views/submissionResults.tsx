import * as React from "react";
import {render} from "react-dom";
import Griddle, {GriddleStyleConfig} from "griddle-react";

import "less/kotoed-bootstrap/bootstrap.less";

let data = [
    {
        "id": 1,
        "first_name": "Barnett",
        "last_name": "Drewell",
        "email": "bdrewell0@umn.edu",
        "gender": "Male",
        "ip_address": "59.167.191.219"
    },
    {
        "id": 2,
        "first_name": "Gillan",
        "last_name": "Rosling",
        "email": "grosling1@goo.ne.jp",
        "gender": "Female",
        "ip_address": "193.136.86.41"
    },
    {
        "id": 3,
        "first_name": "Peadar",
        "last_name": "Eddison",
        "email": "peddison2@imgur.com",
        "gender": "Male",
        "ip_address": "235.40.222.59"
    },
    {
        "id": 4,
        "first_name": "Cecilla",
        "last_name": "Radden",
        "email": "cradden3@creativecommons.org",
        "gender": "Female",
        "ip_address": "170.113.253.254"
    },
    {
        "id": 5,
        "first_name": "Clarice",
        "last_name": "McGrale",
        "email": "cmcgrale4@purevolume.com",
        "gender": "Female",
        "ip_address": "190.49.178.247"
    },
    {
        "id": 6,
        "first_name": "Fletcher",
        "last_name": "Forsbey",
        "email": "fforsbey5@de.vu",
        "gender": "Male",
        "ip_address": "27.24.155.210"
    },
    {
        "id": 7,
        "first_name": "Barnard",
        "last_name": "Lunt",
        "email": "blunt6@wp.com",
        "gender": "Male",
        "ip_address": "77.248.1.192"
    },
    {
        "id": 8,
        "first_name": "Dolf",
        "last_name": "Winterbottom",
        "email": "dwinterbottom7@linkedin.com",
        "gender": "Male",
        "ip_address": "138.68.249.88"
    },
    {
        "id": 9,
        "first_name": "Aura",
        "last_name": "Gorler",
        "email": "agorler8@spotify.com",
        "gender": "Female",
        "ip_address": "150.151.98.210"
    },
    {
        "id": 10,
        "first_name": "Dimitri",
        "last_name": "Kelsow",
        "email": "dkelsow9@thetimes.co.uk",
        "gender": "Male",
        "ip_address": "19.222.234.107"
    },
    {
        "id": 11,
        "first_name": "Kale",
        "last_name": "Beecheno",
        "email": "kbeechenoa@trellian.com",
        "gender": "Male",
        "ip_address": "150.163.191.21"
    },
    {
        "id": 12,
        "first_name": "Marena",
        "last_name": "Jachimiak",
        "email": "mjachimiakb@marriott.com",
        "gender": "Female",
        "ip_address": "118.81.227.196"
    },
    {
        "id": 13,
        "first_name": "Esta",
        "last_name": "Parkyns",
        "email": "eparkynsc@twitpic.com",
        "gender": "Female",
        "ip_address": "53.163.122.147"
    },
    {
        "id": 14,
        "first_name": "Alonzo",
        "last_name": "Gaisford",
        "email": "agaisfordd@vinaora.com",
        "gender": "Male",
        "ip_address": "230.106.188.188"
    },
    {
        "id": 15,
        "first_name": "Rad",
        "last_name": "MacKaig",
        "email": "rmackaige@nasa.gov",
        "gender": "Male",
        "ip_address": "124.255.193.197"
    },
    {
        "id": 16,
        "first_name": "Victoria",
        "last_name": "Paget",
        "email": "vpagetf@epa.gov",
        "gender": "Female",
        "ip_address": "19.99.17.225"
    },
    {
        "id": 17,
        "first_name": "Aloysia",
        "last_name": "Guillem",
        "email": "aguillemg@51.la",
        "gender": "Female",
        "ip_address": "91.65.5.49"
    },
    {
        "id": 18,
        "first_name": "Langsdon",
        "last_name": "Rossin",
        "email": "lrossinh@howstuffworks.com",
        "gender": "Male",
        "ip_address": "32.194.190.44"
    },
    {
        "id": 19,
        "first_name": "Chico",
        "last_name": "Championnet",
        "email": "cchampionneti@ted.com",
        "gender": "Male",
        "ip_address": "5.39.193.234"
    },
    {
        "id": 20,
        "first_name": "Giorgi",
        "last_name": "Escale",
        "email": "gescalej@yelp.com",
        "gender": "Male",
        "ip_address": "19.43.54.172"
    }
];

let awesomeStyleConfig: GriddleStyleConfig = {
    classNames: {
        Table: 'table table-bordered table-striped table-hover'
    }
}

render(
    <Griddle data={data}
             styleConfig={awesomeStyleConfig}/>,
    document.getElementById("view-submission-results")
)
