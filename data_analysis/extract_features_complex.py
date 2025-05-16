"""
Extract typically used eye event features
"""
from __future__ import annotations

import json
import math
import os
import time
import matplotlib.pylab as plt
import numpy as np
import pandas as pd
from scipy.spatial.distance import pdist

# testing the algorithm with an input file

if __name__ == "__main__":
    # image list
    img = [
        "generated_images/HL_12.jpg", "generated_images/LH_20.jpg", "generated_images/LH_05.jpg",
        "generated_images/LH_24.jpg", "generated_images/HL_20.jpg", "generated_images/LH_15.jpg",
        "generated_images/HL_15.jpg", "generated_images/LH_16.jpg", "generated_images/LH_19.jpg",
        "generated_images/HL_29.jpg", "generated_images/HL_06.jpg", "generated_images/LH_09.jpg",
        "generated_images/HL_19.jpg", "generated_images/LH_21.jpg", "generated_images/HL_23.jpg",
        "generated_images/LH_29.jpg", "generated_images/LH_26.jpg", "generated_images/HL_05.jpg",
        "generated_images/LH_04.jpg", "generated_images/LH_03.jpg", "generated_images/HL_11.jpg",
        "generated_images/HL_04.jpg", "generated_images/LH_08.jpg", "generated_images/LH_30.jpg",
        "generated_images/LH_01.jpg", "generated_images/HL_27.jpg", "generated_images/HL_16.jpg",
        "generated_images/LH_27.jpg", "generated_images/HL_07.jpg", "generated_images/LH_28.jpg",
        "generated_images/HL_25.jpg", "generated_images/HL_21.jpg", "generated_images/LH_25.jpg",
        "generated_images/HL_10.jpg", "generated_images/HL_22.jpg", "generated_images/LH_02.jpg",
        "generated_images/LH_22.jpg", "generated_images/HL_09.jpg", "generated_images/HL_28.jpg",
        "generated_images/LH_13.jpg", "generated_images/HL_18.jpg", "generated_images/HL_24.jpg",
        "generated_images/LH_12.jpg", "generated_images/HL_14.jpg", "generated_images/LH_18.jpg",
        "generated_images/LH_17.jpg", "generated_images/HL_03.jpg", "generated_images/LH_07.jpg",
        "generated_images/HL_26.jpg", "generated_images/HL_17.jpg", "generated_images/HL_01.jpg",
        "generated_images/LH_06.jpg", "generated_images/HL_30.jpg", "generated_images/HL_13.jpg",
        "generated_images/LH_23.jpg", "generated_images/LH_11.jpg", "generated_images/LH_10.jpg",
        "generated_images/HL_02.jpg", "generated_images/HL_08.jpg", "generated_images/LH_14.jpg"]

    _im_num = []
    _im_pat = []
    for _im in img:
        _im = _im.replace('generated_images/', '')
        _im = _im.strip('.jpg')
        _tb, _num = _im.split('_')
        # print(_tb, _num)
        _im_num.append(int(_num))
        _im_pat.append(_tb)

    # data frame to store fixation data
    fix_fs = pd.DataFrame(columns=['subj', 'label', 'cesd', 'fix_count_fs',
                                   'fixDuration_mean_fs', 'fixDuration_std_fs',
                                   'x_mean_fs', 'x_std_fs', 'y_mean_fs', 'y_std_fs',
                                   'dispersion_fs'])

    fix_sp = pd.DataFrame(columns=['subj', 'label', 'cesd', 'fix_count_sp',
                                   'fixDuration_mean_sp', 'fixDuration_std_sp',
                                   'x_offset_mean_sp', 'x_offset_std_sp',
                                   'y_offset_mean_sp', 'y_offset_std_sp',
                                   'dispersion_sp'])

    cols = ['subj', 'label', 'cesd']
    for i in range(60):
        # top, bot = _im_pat[i]
        top, bot = ['top', 'bot']
        cols.extend([f'fix_count_{top}_fv_trl_{i}',
                     f'fixDuration_mean_{top}_fv_trl_{i}',
                     f'fixDuration_std_{top}_fv_trl_{i}',
                     f'x_mean_{top}_fv_trl_{i}',
                     f'x_std_{top}_fv_trl_{i}',
                     f'y_mean_{top}_fv_trl_{i}',
                     f'y_std_{top}_fv_trl_{i}',
                     f'fix_count_{bot}_fv_trl_{i}',
                     f'fixDuration_mean_{bot}_fv_trl_{i}',
                     f'fixDuration_std_{bot}_fv_trl_{i}',
                     f'x_mean_{bot}_fv_trl_{i}',
                     f'x_std_{bot}_fv_trl_{i}',
                     f'y_mean_{bot}_fv_trl_{i}',
                     f'y_std_{bot}_fv_trl_{i}',
                     f'x_dist_fv_trl_{i}',
                     f'y_dist_fv_trl_{i}',
                     f'scan_length_fv_trl_{i}',
                     f'ratio_count_fv_trl_{i}',
                     f'ratio_fixDuration_fv_trl_{i}'])

    globl_fv = [
        'fix_count_H_fv',
        'fixDuration_mean_H_fv',
        'fixDuration_std_H_fv',
        # 'x_mean_H_fv',
        # 'x_std_H_fv',
        # 'y_mean_H_fv',
        # 'y_std_H_fv',
        'fix_count_L_fv',
        'fixDuration_mean_L_fv',
        'fixDuration_std_L_fv',
        # 'x_mean_L_fv',
        # 'x_std_L_fv',
        # 'y_mean_L_fv',
        # 'y_std_L_fv',
        'dispersion_fv',
        'count_ratio_fv',
        'fixDuration_ratio_fv',
        'scan_length_mean_fv',
        'scan_length_std_fv']

    cols.extend(globl_fv)
    fix_fv = pd.DataFrame(columns=cols)
    print(len(fix_fv.columns), 'column names', len(globl_fv))

    # data frame to store saccade data
    sac_fs = pd.DataFrame(columns=['subj', 'label', 'cesd', 'sac_count_fs',
                                   'sacDuration_mean_fs', 'sacDuration_std_fs',
                                   'sacAmp_mean_fs', 'sacAmp_std_fs',
                                   'sacVel_mean_fs', 'sacVel_std_fs'])

    sac_sp = pd.DataFrame(columns=['subj', 'label', 'cesd', 'sp_sac_count_sp',
                                   'sp_sacDuration_mean_sp', 'sp_sacDuration_std_sp',
                                   'sp_sacAmp_mean_sp', 'sp_sacAmp_std_sp',
                                   'sp_sacVel_mean_sp', 'sp_sacVel_std_sp'])

    sac_fv = pd.DataFrame(columns=['subj', 'label', 'cesd', 'sac_count_fv',
                                   'sacDuration_mean_fv', 'sacDuration_std_fv',
                                   'sacAmp_mean_fv', 'sacAmp_std_fv',
                                   'sacVel_mean_fv', 'sacVel_std_fv'])

    # retrieve the ces-d scores and group label
    _score = pd.read_csv('label.csv')
    # print(type(_score['subj'][0]))

    # folder that stores the detected eye event files
    ev_folder = os.path.join(os.getcwd(), 'events')
    _ev_files = os.listdir(ev_folder)
    for _file in _ev_files:
        if _file == ".DS_Store":
            continue

        print(f'processing file {_file}')

        # get task, subj #, and data type from the file name
        _dt, _subj, _task = _file.split('.')[0].split('_')

        # get data label and ces-d score
        _label = _score.loc[_score['subj'] == int(_subj), 'label'].values[0]
        _ces_d = _score.loc[_score['subj'] == int(_subj), 'ced_score'].values[0]

        # full path to the data file
        _path = os.path.join(ev_folder, _file)

        # processing fixation data 
        if _dt == 'fix':
            # read in the fixation data file
            _fix = pd.read_csv(_path)

            # calculate some global measures for the smooth pursuit and fixation stability tasks
            _fix_count = len(_fix)
            _fix_duration_mean = np.nanmean(_fix['duration'])
            _fix_duration_std = np.nanstd(_fix['duration'])
            _fix_avg_x_mean = np.nanmean(_fix['avg_x'])
            _fix_avg_x_std = np.nanstd(_fix['avg_x'])
            _fix_avg_y_mean = np.nanmean(_fix['avg_y'])
            _fix_avg_y_std = np.nanstd(_fix['avg_y'])
            _fix_dispersion = np.nanmean(pdist(_fix[['avg_x', 'avg_y']], metric='euclidean'))

            if _fix_count == 0:
                _fix_duration_mean = 0.0
                _fix_duration_std = 0.0
                _fix_avg_x_mean = 0.0
                _fix_avg_x_std = 0.0
                _fix_avg_y_mean = 0.0
                _fix_avg_y_std = 0.0
                _fix_dispersion = 0.0

            if _task == 'fs':  # record data for the fixation stability task
                fix_fs.loc[len(fix_fs)] = [
                    _subj,
                    _label,
                    _ces_d,
                    _fix_count,
                    _fix_duration_mean,
                    _fix_duration_std,
                    _fix_avg_x_mean,
                    _fix_avg_x_std,
                    _fix_avg_y_mean,
                    _fix_avg_y_std,
                    _fix_dispersion]

            if _task == 'sp':  # record data for the smooth pursuit task
                _sp_path = os.path.join(ev_folder, _file.replace('fix', 'sp'))
                _sp = pd.read_csv(_sp_path)
                fix_sp.loc[len(fix_sp)] = [
                    _subj,
                    _label,
                    _ces_d,
                    _fix_count,
                    _fix_duration_mean,
                    _fix_duration_std,
                    np.mean(_sp['offset_x_mean']),
                    np.mean(_sp['offset_x_std']),
                    np.mean(_sp['offset_y_mean']),
                    np.mean(_sp['offset_y_std']),
                    _fix_dispersion]

            if _task == 'fv':  # record data for the smooth pursuit task

                # fixation stats for the FV task
                _fix_count_h = 0
                _fix_duration_h = []
                _fix_avg_x_h = []
                _fix_avg_y_h = []
                _fix_count_l = 0
                _fix_duration_l = []
                _fix_avg_x_l = []
                _fix_avg_y_l = []
                _fix_scan_len = []

                _features = [
                    _subj,
                    _label,
                    _ces_d
                ]

                for i in range(60):
                    _trial_start = i * 3500 + 500
                    _trial_end = (i + 1) * 3500
                    # extract rows that belong to a single trial
                    _td = _fix.loc[(_fix['offset'] >= _trial_start) & (_fix['offset'] <= _trial_end)]
                    # extract rows with fixation in the top AOI
                    _top = _td.loc[_td['avg_y'] < 11.5]
                    _bot = _td.loc[_td['avg_y'] >= 11.5]

                    # global high and low arousal AOI stats
                    if _im_pat[i] == 'HL':
                        _fix_count_h += len(_top)
                        _fix_duration_h.extend(list(_top['duration']))
                        _fix_avg_x_h.extend(list(_top['avg_x']))
                        _fix_avg_y_h.extend(list(_top['avg_y']))
                        _fix_count_l += len(_bot)
                        _fix_duration_l.extend(list(_bot['duration']))
                        _fix_avg_x_l.extend(list(_bot['avg_x']))
                        _fix_avg_y_l.extend(list(_bot['avg_y']))

                    if _im_pat[i] == 'LH':
                        _fix_count_l += len(_top)
                        _fix_duration_l.extend(list(_top['duration']))
                        _fix_avg_x_l.extend(list(_top['avg_x']))
                        _fix_avg_y_l.extend(list(_top['avg_y']))
                        _fix_count_h += len(_bot)
                        _fix_duration_h.extend(list(_bot['duration']))
                        _fix_avg_x_h.extend(list(_bot['avg_x']))
                        _fix_avg_y_h.extend(list(_bot['avg_y']))

                    # get the scan length
                    _scan_length = np.sum(np.sqrt(np.diff(_td['avg_x']) ** 2 + np.diff(_td['avg_y']) ** 2))
                    _fix_scan_len.append(_scan_length)

                    # image-wise stats
                    _tmp = [len(_top),
                            np.mean(_top['duration']),
                            np.std(_top['duration']),
                            np.mean(_top['avg_x']),
                            np.std(_top['avg_x']),
                            np.mean(_top['avg_y']),
                            np.std(_top['avg_y']),
                            len(_bot),
                            np.mean(_bot['duration']),
                            np.std(_bot['duration']),
                            np.mean(_bot['avg_x']),
                            np.std(_bot['avg_x']),
                            np.mean(_bot['avg_y']),
                            np.std(_bot['avg_y']),
                            # distance
                            np.abs(np.mean(_top['avg_x']) - np.mean(_bot['avg_x'])),
                            np.abs(np.mean(_top['avg_y']) - np.mean(_bot['avg_y'])),
                            # scan length
                            _scan_length,
                            # ratio
                            _fix_count_l / (_fix_count_h + _fix_count_h),
                            np.sum(_fix_duration_l) / (np.sum(_fix_duration_h) + np.sum(_fix_duration_l))
                            ]

                    if _tmp[0] == 0:
                        _tmp[1] = 0
                        _tmp[2] = 0
                        _tmp[3] = 0
                        _tmp[4] = 0
                        _tmp[5] = 0
                        _tmp[6] = 0
                        _tmp[14] = 0
                        _tmp[15] = 0
                        _tmp[16] = 0
                        _tmp[17] = 0
                        _tmp[18] = 0

                    if _tmp[7] == 0:
                        _tmp[8] = 0
                        _tmp[9] = 0
                        _tmp[10] = 0
                        _tmp[11] = 0
                        _tmp[12] = 0
                        _tmp[13] = 0
                        _tmp[14] = 0
                        _tmp[15] = 0
                        _tmp[16] = 0
                        _tmp[17] = 0
                        _tmp[18] = 0

                    _features.extend(_tmp)

                _exp_features = [
                    _fix_count_l,
                    np.nanmean(_fix_duration_l),
                    np.nanstd(_fix_duration_l),
                    # np.nanmean(_fix_avg_x_l),
                    # np.nanstd(_fix_avg_x_l),
                    # np.nanmean(_fix_avg_y_l),
                    # np.nanstd(_fix_avg_y_l),
                    _fix_count_h,
                    np.nanmean(_fix_duration_h),
                    np.nanstd(_fix_duration_h),
                    # np.nanmean(_fix_avg_x_h),
                    # np.nanstd(_fix_avg_x_h),
                    # np.nanmean(_fix_avg_y_h),
                    # np.nanstd(_fix_avg_y_h),
                    _fix_dispersion,
                    _fix_count_l / _fix_count_h,
                    np.sum(_fix_duration_l) / np.sum(_fix_duration_h),
                    np.nanmean(_fix_scan_len),
                    np.nanstd(_fix_scan_len)
                ]

                _features.extend(_exp_features)

                print(len(fix_fv.columns), len(_features), len(_exp_features))

                fix_fv.loc[len(fix_fv)] = _features

        # processing saccade data
        if _dt == 'sac':
            # read in the fixation data file
            _sac = pd.read_csv(_path)
            _sac_count = len(_sac['duration'])
            _sac_amp_mean = np.mean(_sac['sac_amp'])
            _sac_amp_std = np.std(_sac['sac_amp'])

            _sac_duration_mean = np.mean(_sac['duration'])
            _sac_duration_std = np.std(_sac['duration'])
            _sac['vel'] = _sac['sac_amp'] / _sac['duration']  # velocity
            _sac_vel_mean = np.mean(_sac['vel'])
            _sac_vel_std = np.std(_sac['vel'])

            if _task == 'fs':
                sac_fs.loc[len(sac_fs)] = [
                    _subj,
                    _label,
                    _ces_d,
                    _sac_count,
                    _sac_duration_mean,
                    _sac_duration_std,
                    _sac_amp_mean,
                    _sac_amp_std,
                    _sac_vel_mean,
                    _sac_vel_std]

            if _task == 'sp':
                sac_sp.loc[len(sac_sp)] = [
                    _subj,
                    _label,
                    _ces_d,
                    _sac_count,
                    _sac_duration_mean,
                    _sac_duration_std,
                    _sac_amp_mean,
                    _sac_amp_std,
                    _sac_vel_mean,
                    _sac_vel_std]

            if _task == 'fv':
                sac_fv.loc[len(sac_fv)] = [
                    _subj,
                    _label,
                    _ces_d,
                    _sac_count,
                    _sac_duration_mean,
                    _sac_duration_std,
                    _sac_amp_mean,
                    _sac_amp_std,
                    _sac_vel_mean,
                    _sac_vel_std]

        # # save results to csv
        _fs_fix_path = os.path.join(os.getcwd(), 'fs_fixation_features.csv')
        _sp_fix_path = os.path.join(os.getcwd(), 'sp_fixation_features.csv')
        _fv_fix_path = os.path.join(os.getcwd(), 'fv_fixation_features.csv')
        _fs_sac_path = os.path.join(os.getcwd(), 'fs_saccade_features.csv')
        _sp_sac_path = os.path.join(os.getcwd(), 'sp_saccade_features.csv')
        _fv_sac_path = os.path.join(os.getcwd(), 'fv_saccade_features.csv')
        fix_fs.to_csv(_fs_fix_path, index=False)
        fix_sp.to_csv(_sp_fix_path, index=False)
        fix_fv.to_csv(_fv_fix_path, index=False)
        sac_fs.to_csv(_fs_sac_path, index=False)
        sac_sp.to_csv(_sp_sac_path, index=False)
        sac_fv.to_csv(_fv_sac_path, index=False)
