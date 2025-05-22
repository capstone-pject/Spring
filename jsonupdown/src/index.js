import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import Login from './Login';
import Join from './Join'
import reportWebVitals from './reportWebVitals';
import Calendar from './Calendar';
import DrugSearch from './DrugSearch';
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
 {/* 필요하다면 Login, Join 컴포넌트도 여기에 포함시킬 수 있습니다. */}
      <Join /> 
      <Login /> 
      <Calendar userId="a" />
      <DrugSearch />
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
