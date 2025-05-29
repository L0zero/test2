
import axios from 'axios'
import { BASE_URL, TIME_OUT } from '../config/config';

const request = axios.create({
    // baseURL: BASE_URL, // url = base url + request url
    baseURL: 'http://8.140.206.102:8000/',
    // send cookies when cross-domain requests
    timeout: TIME_OUT // request timeout
});

export default request;
